# Justificación Técnica — Corrección de Bugs

Este documento detalla la justificación técnica y las decisiones de diseño detrás de cada corrección aplicada en el laboratorio de práctica deliberada.

---

## Bug 1 — Performance: N+1 queries en listado de tareas

### Problema
El endpoint `GET /api/tasks/project/{id}` ejecutaba `findAll()` cargando todas las tareas de todos los proyectos en memoria, y luego filtraba con `.filter()` en Java. Además, por cada tarea, se hacía una llamada HTTP individual al user-service para obtener el nombre del asignado.

### Justificación de la solución

**1. JOIN FETCH en JPQL:**
Se creó `findByProjectIdOptimized()` con `JOIN FETCH t.project` porque:
- Delega el filtrado a la base de datos (WHERE en SQL), eliminando la carga innecesaria en memoria
- El `JOIN FETCH` resuelve la relación LAZY en una sola query, evitando el N+1 de Hibernate
- Es la solución estándar de JPA para el problema N+1 en relaciones LAZY

**2. Pre-carga batch de nombres de usuario:**
Se extraen los `assigneeId` únicos con `.distinct()` y se resuelven todos antes de mapear las tareas porque:
- Reduce N llamadas HTTP a K (donde K = número de usuarios únicos, K << N)
- Con 16 tareas y 2 usuarios: de 16 llamadas HTTP a solo 2
- El `Collectors.toMap()` crea un lookup O(1) para el mapeo posterior

**3. Método `toResponseWithName`:**
Se creó un método separado que recibe el nombre pre-resuelto en vez de llamar al user-service, aplicando el principio de separación de responsabilidades.

### Complejidad resultante
- Antes: O(N) queries SQL + O(N) llamadas HTTP
- Después: O(1) query SQL + O(K) llamadas HTTP (K = usuarios únicos)

---

## Bug 2 — Concurrencia: Race condition en cambio de estado

### Problema
Dos usuarios podían leer la misma tarea simultáneamente, modificar su estado, y el último en escribir sobreescribía al primero sin ninguna detección de conflicto.

### Justificación de la solución

**1. `@Version` (Optimistic Locking):**
Se eligió optimistic locking sobre pessimistic locking porque:
- En una aplicación web, la contención es baja (es raro que dos usuarios editen la misma tarea al mismo milisegundo)
- No bloquea registros en la BD, manteniendo alto throughput
- JPA lo implementa automáticamente con `@Version`: agrega `AND version = ?` al UPDATE
- Si el UPDATE afecta 0 filas, JPA lanza `OptimisticLockException`

**2. HTTP 409 Conflict:**
Se eligió 409 sobre 400 o 500 porque:
- 409 es semánticamente correcto: "el request no pudo completarse debido a un conflicto con el estado actual del recurso"
- Permite al frontend distinguir entre un error de validación (400) y un conflicto de concurrencia (409)
- El cliente puede reintentar automáticamente o mostrar un mensaje específico al usuario

**3. Mensaje descriptivo:**
El mensaje "El registro fue modificado por otro usuario" informa al usuario exactamente qué pasó y qué hacer (recargar e intentar de nuevo).

---

## Bug 3 — Resiliencia: Fallo en cascada entre microservicios

### Problema
Si el user-service se caía o no respondía, el task-service se bloqueaba indefinidamente y todas sus operaciones fallaban con 500, incluso las que no necesitaban datos de usuario.

### Justificación de la solución

**1. Timeout (3 segundos):**
- Evita que los hilos de Tomcat se bloqueen indefinidamente
- 3 segundos es un balance entre dar tiempo suficiente al user-service y no degradar la experiencia del usuario
- Sin timeout, un servicio lento puede agotar el thread pool completo de Tomcat

**2. Circuit Breaker (Resilience4j):**
Se eligió el patrón Circuit Breaker porque:
- Detecta fallos sostenidos y deja de intentar llamadas que van a fallar (fail fast)
- Reduce la carga sobre un servicio que está recuperándose
- Los 3 estados (CLOSED → OPEN → HALF_OPEN) permiten recuperación automática
- Configuración: 50% failure rate con ventana de 5 llamadas = abre después de 3 fallos en 5 intentos

**3. Retry con Backoff Exponencial:**
- Maneja fallos transitorios (glitch de red momentáneo) sin ir directo al fallback
- Backoff exponencial (1s → 2s → 4s) evita saturar un servicio que está bajo presión
- Solo reintenta en `ResourceAccessException` (errores de red), no en errores de negocio (404, 400)
- Se ejecuta ANTES del Circuit Breaker: si los 3 reintentos fallan, el CB registra 1 fallo

**4. Fallback "Unknown User":**
- Degradación graceful: el sistema sigue funcionando con información parcial
- El usuario puede ver y gestionar sus tareas aunque no vea los nombres de los asignados
- Mejor que un error 500 que bloquea toda la funcionalidad

### Orden de ejecución
```
Request → Retry (3 intentos) → Circuit Breaker → Fallback
```

---

## Bug 4 — Cache: Datos stale después de actualizar

### Problema
Después de crear o actualizar una tarea, las consultas seguían devolviendo datos antiguos de la caché de Redis durante hasta 10 minutos (TTL configurado).

### Justificación de la solución

**1. `@CacheEvict` en operaciones de escritura:**
Se eligió invalidación sobre actualización de caché porque:
- Es más simple y menos propenso a errores que intentar actualizar la caché manualmente
- `allEntries = true` garantiza consistencia: cualquier escritura invalida toda la caché relacionada
- El siguiente GET reconstruye la caché con datos frescos

**2. Invalidar ambas cachés (`tasks` + `tasksByProject`):**
- `tasks`: caché por ID individual. Si actualizas el status de tarea 5, `getById(5)` debe reflejar el cambio
- `tasksByProject`: caché de listado. Si creas una tarea en proyecto 1, el listado debe incluirla
- Invalidar ambas garantiza consistencia en todos los endpoints de lectura

**3. ¿Por qué no `@CachePut`?**
- `@CachePut` actualiza una entrada específica, pero `getByProject()` cachea una lista completa
- Actualizar una tarea individual no permite reconstruir la lista sin re-ejecutar la query
- La invalidación es más segura: fuerza un refetch que siempre es correcto

---

## Bug 5 — Auth: Token refresh causa loop infinito

### Problema
Cuando múltiples peticiones recibían 401 simultáneamente, cada una intentaba hacer refresh de forma independiente, generando un bucle infinito de requests.

### Justificación de la solución

**1. Flag `_retry` por request:**
- Marca cada request individual para que solo intente refresh UNA vez
- Si la petición reintentada vuelve a recibir 401, no se vuelve a intentar → se propaga el error
- Es la solución mínima y efectiva para romper el loop

**2. Axios puro para el refresh:**
- Se usa `axios.post(...)` directamente en vez de `taskApi` o `authApi`
- Esto evita que el interceptor de respuesta capture un posible error del refresh y genere recursión
- El refresh es una operación atómica que no debe ser interceptada

**3. Actualización atómica del storage:**
- Se actualiza `auth-storage` completo en localStorage de una sola vez
- Esto garantiza que todas las peticiones subsiguientes usen el nuevo token
- Si el refresh falla, se limpia todo el storage y se redirige a login (logout definitivo)

### Alternativa no implementada (mejora futura)
Una cola de peticiones (`failedQueue`) donde solo la primera petición hace refresh y las demás esperan. Esto es más eficiente con muchas peticiones simultáneas pero más complejo de implementar.

---

## Bug 6 — UX: Tarea desaparece al fallar drag & drop

### Problema
El optimistic update movía la tarea en la UI inmediatamente, pero si la API fallaba, no había rollback ni resincronización con el servidor.

### Justificación de la solución

**1. Snapshot previo (`previousTasks`):**
- Se guarda el estado completo de la caché ANTES del optimistic update
- Esto permite restaurar exactamente el estado anterior si algo falla
- Es el patrón estándar de React Query para optimistic updates

**2. Rollback en `onError`:**
- Si la mutación falla, se restaura el snapshot previo inmediatamente
- El usuario ve la tarea volver a su posición original sin necesidad de refrescar
- Se usa el `context` retornado por `onMutate` para acceder al snapshot

**3. `invalidateQueries` en `onSettled`:**
- Se ejecuta SIEMPRE (éxito o fallo) para garantizar consistencia con el servidor
- Incluso en éxito, el servidor puede haber aplicado transformaciones (timestamps, validaciones)
- Fuerza un refetch silencioso que sincroniza la UI con la verdad del servidor

### Patrón completo
```
onMutate: snapshot + optimistic update + return context
onError: rollback con context
onSettled: invalidate queries (resync)
```

---

## Bug 7 — Memory Leak: Polling sin cleanup

### Problema
El `setInterval` del Dashboard seguía ejecutándose después de que el usuario navegaba a otra página, causando actualizaciones de estado en un componente desmontado.

### Justificación de la solución

**1. Cleanup function en `useEffect`:**
- React ejecuta la función retornada por `useEffect` cuando el componente se desmonta
- `clearInterval(interval)` detiene el polling inmediatamente
- Es el contrato fundamental de `useEffect`: todo lo que se crea debe limpiarse

**2. ¿Por qué es un memory leak?**
- El `setInterval` mantiene una referencia al closure de `fetchStats`
- `fetchStats` referencia a `setStats` (el setter del estado)
- Mientras el intervalo exista, el garbage collector no puede liberar esa memoria
- Cada visita al Dashboard sin cleanup crea un nuevo intervalo que nunca se limpia

**3. Impacto acumulativo:**
- Visita 1: 1 intervalo activo
- Visita 2: 2 intervalos activos (el anterior nunca se limpió)
- Visita N: N intervalos haciendo requests cada 5 segundos
- Eventualmente: degradación de performance y posible crash del navegador

---

## Resumen de patrones aplicados

| Bug | Patrón/Principio |
|-----|-----------------|
| Bug 1 | JOIN FETCH + Batch Loading |
| Bug 2 | Optimistic Locking (JPA @Version) |
| Bug 3 | Circuit Breaker + Retry + Fallback + Timeout |
| Bug 4 | Cache Invalidation (Write-through) |
| Bug 5 | Idempotent Retry Flag |
| Bug 6 | Optimistic Update with Rollback |
| Bug 7 | Resource Cleanup (useEffect contract) |
