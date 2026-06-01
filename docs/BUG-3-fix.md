# Bug 3 — Resiliencia: Fallo en cascada entre microservicios

## Síntoma
Si el user-service se cae o no responde, TODAS las operaciones del task-service fallan con HTTP 500, incluso las que no necesitan datos de usuario (como listar tareas). El task-service se bloquea indefinidamente esperando respuesta.

## Ubicación
- `backend/task-service/src/.../infrastructure/client/UserServiceClient.java`
- `backend/task-service/src/.../TaskServiceApplication.java`

## Causa raíz
1. El `RestTemplate` no tenía timeout configurado — si el user-service estaba colgado (no caído, sino lento), el hilo se bloqueaba para siempre.
2. No había Circuit Breaker ni fallback — cualquier excepción del `RestTemplate` se propagaba directamente al caller (`TaskService.toResponse()`), rompiendo toda la operación.

### Código con el bug
```java
// TaskServiceApplication.java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate(); // Sin timeout
}

// UserServiceClient.java
public String getUserName(Long userId) {
    String url = userServiceUrl + "/api/users/" + userId + "/name";
    Map<String, String> response = restTemplate.getForObject(url, Map.class); // Sin fallback
    return response != null ? response.get("name") : "Unknown";
}
```

## Corrección aplicada

### 1. Timeout en RestTemplate (`TaskServiceApplication.java`)
```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);  // 3 segundos para establecer conexión
    factory.setReadTimeout(3000);     // 3 segundos para leer respuesta
    return new RestTemplate(factory);
}
```

### 2. Circuit Breaker + Fallback (`UserServiceClient.java`)
```java
@CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserName")
public String getUserName(Long userId) {
    String url = userServiceUrl + "/api/users/" + userId + "/name";
    Map<String, String> response = restTemplate.getForObject(url, Map.class);
    return response != null ? response.get("name") : "Unknown";
}

public String fallbackGetUserName(Long id, Throwable t) {
    System.out.println("CircuitBreaker OPEN - user-service caído. Error: " + t.getMessage());
    return "Unknown User";
}
```

### 3. Configuración de Resilience4j (`application.yml`)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        registerHealthIndicator: true
        slidingWindowSize: 5
        minimumNumberOfCalls: 3
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
  retry:
    instances:
      userService:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - org.springframework.web.client.ResourceAccessException
```

### 4. Retry con Backoff Exponencial (`UserServiceClient.java`)
```java
@Retry(name = "userService")
@CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserName")
public String getUserName(Long userId) { ... }
```

El `@Retry` se ejecuta antes que el `@CircuitBreaker`:
- Si la llamada falla, reintenta hasta 3 veces con espera creciente (1s → 2s → 4s)
- Solo reintenta en `ResourceAccessException` (errores de red/timeout), no en errores de negocio
- Si los 3 intentos fallan, el Circuit Breaker registra el fallo y eventualmente abre el circuito
- Con el circuito OPEN, no se ejecuta Retry — va directo al fallback

## Conceptos clave

### Circuit Breaker (patrón de resiliencia)
Funciona como un interruptor eléctrico con 3 estados:
- **CLOSED** → Funcionamiento normal, las llamadas pasan al user-service
- **OPEN** → Demasiados fallos detectados, las llamadas van directo al fallback sin intentar la conexión
- **HALF_OPEN** → Después de `waitDurationInOpenState` (10s), permite algunas llamadas de prueba para verificar si el servicio se recuperó

### Degradación graceful
En lugar de fallar completamente, el sistema se degrada mostrando "Unknown User" donde normalmente iría el nombre real. El usuario puede seguir trabajando con el task-service.

## Verificación
1. Detener el user-service (`Ctrl+C` en su terminal)
2. Hacer `GET http://localhost:8081/api/tasks/project/1` → responde 200 con `assigneeName: "Unknown User"`
3. En los logs del task-service se ve: `CircuitBreaker OPEN - user-service caído`
4. Reiniciar user-service → después de 10s el circuit breaker pasa a HALF_OPEN y eventualmente a CLOSED

## Impacto de no corregirlo
- Un microservicio caído tumba a todos los que dependen de él (efecto dominó)
- Los hilos del task-service se bloquean indefinidamente, agotando el thread pool de Tomcat
- El sistema completo se vuelve inutilizable por un solo punto de fallo
