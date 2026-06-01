# Bug 4 — Cache: Datos stale después de actualizar

## Síntoma
Después de crear o actualizar una tarea, si consultas inmediatamente el listado, ves los datos antiguos. Hay que esperar ~10 minutos (TTL del cache en Redis) para ver los cambios reflejados.

## Ubicación
`backend/task-service/src/.../application/service/TaskService.java`

## Causa raíz
Los métodos `create()`, `updateStatus()` y `assign()` modifican datos en la base de datos, pero no invalidan las entradas correspondientes en la caché de Redis. Los métodos de lectura (`getById()` y `getByProject()`) usan `@Cacheable`, lo que significa que una vez cacheada la respuesta, se sirve desde Redis sin consultar la BD hasta que expire el TTL.

### Código con el bug
```java
@Transactional
public TaskResponse create(TaskRequest request) {
    // ... crea la tarea en BD
    Task saved = taskRepository.save(task);
    return toResponse(saved);
    // La caché "tasksByProject" sigue sirviendo la lista vieja (sin esta nueva tarea)
}

@Transactional
public TaskResponse updateStatus(Long id, String status) {
    // ... actualiza el status en BD
    Task updated = taskRepository.save(task);
    return toResponse(updated);
    // La caché "tasks" sigue sirviendo el status viejo
}
```

## Corrección aplicada
Se agregó `@CacheEvict` en los 3 métodos que modifican datos, invalidando ambas cachés:

```java
@Transactional
@CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
public TaskResponse create(TaskRequest request) { /* ... */ }

@Transactional
@CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
public TaskResponse updateStatus(Long id, String status) { /* ... */ }

@Transactional
@CacheEvict(value = {"tasks", "tasksByProject"}, allEntries = true)
public TaskResponse assign(Long id, Long assigneeId) { /* ... */ }
```

## Conceptos clave

### Estrategia de invalidación de caché
- `@Cacheable` → Almacena el resultado en caché. Las siguientes llamadas con la misma key se sirven desde Redis.
- `@CacheEvict` → Elimina entradas de la caché. Se usa en operaciones de escritura para garantizar consistencia.
- `allEntries = true` → Invalida TODAS las entradas de esa caché (no solo una key específica). Es la estrategia más segura cuando una escritura puede afectar múltiples lecturas.

### ¿Por qué invalidar ambas cachés?
- `"tasks"` → Caché por ID individual (`getById()`). Si actualizas el status de la tarea 5, la entrada cacheada de `getById(5)` queda obsoleta.
- `"tasksByProject"` → Caché de listado por proyecto (`getByProject()`). Si creas una tarea nueva en el proyecto 1, la lista cacheada del proyecto 1 no la incluye.

## Verificación
1. `GET /api/tasks/project/1` → responde con N tareas (se cachea)
2. `POST /api/tasks` → crear nueva tarea en proyecto 1
3. `GET /api/tasks/project/1` → ahora muestra N+1 tareas inmediatamente (caché invalidada)

## Impacto de no corregirlo
- Los usuarios ven datos desactualizados durante hasta 10 minutos
- Confusión: "creé una tarea pero no aparece"
- En un Kanban, mover una tarea y refrescar muestra el estado anterior
- Inconsistencia entre lo que el usuario hizo y lo que la UI muestra
