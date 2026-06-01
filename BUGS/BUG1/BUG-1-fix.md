# Bug 1 — Performance: N+1 queries en listado de tareas

## Síntoma
El endpoint `GET /api/tasks/project/{id}` tarda >2 segundos con 15+ tareas. En los logs se ven decenas de queries SQL repetidas (`SELECT ... FROM projects WHERE id=?` por cada tarea).

## Ubicación
- `backend/task-service/src/.../application/service/TaskService.java`
- `backend/task-service/src/.../domain/repository/TaskRepository.java`
- `backend/task-service/src/.../domain/model/Task.java`

## Causa raíz
Tres problemas combinados:

1. **`findAll()` en vez de filtrar por proyecto:** Se cargaban TODAS las tareas de TODOS los proyectos y se filtraban en memoria con `.filter()`.
2. **N+1 SQL (LAZY loading):** Con `FetchType.LAZY` en la relación `Task → Project`, cada acceso a `task.getProject().getName()` disparaba una query SQL adicional.
3. **N+1 HTTP (user-service):** `toResponse()` llamaba a `userServiceClient.getUserName()` individualmente por cada tarea, generando N llamadas HTTP al user-service.

### Código con el bug
```java
public List<TaskResponse> getByProject(Long projectId) {
    List<Task> allTasks = taskRepository.findAll(); // Carga TODAS las tareas
    return allTasks.stream()
            .filter(t -> t.getProject().getId().equals(projectId)) // Filtra en memoria
            .map(this::toResponse) // N+1: cada llamada hace HTTP al user-service
            .collect(Collectors.toList());
}
```

## Corrección aplicada

### 1. Query optimizada con JOIN FETCH (`TaskRepository.java`)
```java
@Query("SELECT t FROM Task t JOIN FETCH t.project WHERE t.project.id = :projectId")
List<Task> findByProjectIdOptimized(@Param("projectId") Long projectId);
```
- Filtra por proyecto directamente en SQL (no en memoria)
- `JOIN FETCH` carga el Project en la misma query (elimina N+1 SQL)

### 2. Pre-carga batch de nombres de usuario (`TaskService.java`)
```java
public List<TaskResponse> getByProject(Long projectId) {
    List<Task> allTasks = taskRepository.findByProjectIdOptimized(projectId);

    // Obtener IDs únicos de asignados (evita llamadas duplicadas)
    Map<Long, String> userNames = allTasks.stream()
            .map(Task::getAssigneeId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toMap(id -> id, id -> userServiceClient.getUserName(id)));

    // Mapear tareas usando el mapa pre-cargado (0 llamadas HTTP adicionales)
    return allTasks.stream()
            .map(task -> toResponseWithName(task, userNames.getOrDefault(task.getAssigneeId(), null)))
            .collect(Collectors.toList());
}
```

### 3. Método `toResponseWithName` sin llamada HTTP
```java
private TaskResponse toResponseWithName(Task task, String assigneeName) {
    return TaskResponse.builder()
            .id(task.getId())
            // ... campos
            .assigneeName(assigneeName) // Usa el nombre pre-cargado
            .build();
}
```

## Conceptos clave

### N+1 Problem
- **1 query** para obtener la lista principal
- **N queries** adicionales para cargar datos relacionados de cada elemento
- Solución: `JOIN FETCH` (SQL) o batch loading (HTTP)

### JOIN FETCH vs EAGER
- `FetchType.EAGER` carga siempre la relación (incluso cuando no la necesitas)
- `JOIN FETCH` en JPQL carga la relación solo cuando lo pides explícitamente en esa query

## Resultado
| Métrica | Antes | Después |
|---|---|---|
| Queries SQL | 1 (findAll) + N (projects) | 1 (JOIN FETCH) |
| Llamadas HTTP al user-service | N (una por tarea) | K (solo IDs únicos, K << N) |
| Datos cargados en memoria | Todas las tareas de todos los proyectos | Solo las del proyecto solicitado |

Con 16 tareas y 2 usuarios: de **16 queries SQL + 16 HTTP** a **1 query SQL + 2 HTTP**.

## Verificación
1. Limpiar Redis: `docker exec redis-kanban redis-cli FLUSHALL`
2. `GET /api/tasks/project/1`
3. En logs: solo 1 query `SELECT t ... JOIN ... projects` (no más N+1)
4. Tiempo de respuesta significativamente menor
