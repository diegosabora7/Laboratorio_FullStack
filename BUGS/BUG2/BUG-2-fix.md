# Bug 2 — Concurrencia: Race condition en cambio de estado

## Síntoma
Cuando dos usuarios mueven la misma tarea simultáneamente en el Kanban (ej: uno la mueve a IN_PROGRESS y otro a IN_REVIEW), una de las actualizaciones se pierde silenciosamente. No hay error ni notificación — el último en escribir gana.

## Ubicación
- `backend/task-service/src/.../domain/model/Task.java`
- `backend/task-service/src/.../infrastructure/web/GlobalExceptionHandler.java`

## Causa raíz
La entidad `Task` no tenía un campo `@Version` para optimistic locking. Sin este mecanismo, JPA simplemente ejecuta un `UPDATE` sin verificar si el registro fue modificado entre el momento de la lectura y la escritura. Dos transacciones concurrentes leen la misma versión, ambas escriben, y la segunda sobreescribe a la primera sin detectar el conflicto.

### Código con el bug
```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    private Long id;
    private TaskStatus status;
    // Sin @Version — no hay detección de conflictos
}
```

## Corrección aplicada

### 1. Optimistic Locking en la entidad (`Task.java`)
```java
@Version
private Long version;
```

JPA ahora incluye la versión en el `WHERE` del `UPDATE`:
```sql
UPDATE tasks SET status = ?, version = version + 1 WHERE id = ? AND version = ?
```
Si otro usuario ya modificó el registro (version cambió), el UPDATE afecta 0 filas y JPA lanza `OptimisticLockException`.

### 2. Manejo de la excepción (`GlobalExceptionHandler.java`)
```java
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<String> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)  // 409
            .body("El registro fue modificado por otro usuario, por favor recargue la página e intente nuevamente");
}
```

## Conceptos clave

### Optimistic Locking vs Pessimistic Locking
- **Optimistic** (usado aquí): Asume que los conflictos son raros. Permite lecturas concurrentes y detecta conflictos al momento de escribir. Ideal para aplicaciones web con baja contención.
- **Pessimistic**: Bloquea el registro al leerlo (`SELECT ... FOR UPDATE`). Garantiza exclusividad pero reduce throughput y puede causar deadlocks.

### Flujo con la corrección
1. Usuario A lee tarea (version = 1)
2. Usuario B lee tarea (version = 1)
3. Usuario A actualiza → `UPDATE ... WHERE version = 1` → éxito, version = 2
4. Usuario B actualiza → `UPDATE ... WHERE version = 1` → 0 filas afectadas → `OptimisticLockException` → HTTP 409

## Verificación
1. Obtener una tarea: `GET /api/tasks/{id}`
2. Enviar dos PATCH simultáneos con diferentes status al mismo ID
3. Uno responde 200 OK, el otro responde 409 Conflict con mensaje descriptivo

## Impacto de no corregirlo
- Pérdida silenciosa de datos — el usuario cree que su cambio se guardó pero fue sobreescrito
- En un Kanban colaborativo, las tareas "saltan" de columna inesperadamente
- Imposible auditar quién hizo qué cambio si se pierden actualizaciones
