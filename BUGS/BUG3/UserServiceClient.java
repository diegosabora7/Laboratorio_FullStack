package com.taskmanager.taskservice.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    // TODO: [LABORATORIO - BUG 3] - Fallo en cascada: sin timeout, sin circuit breaker, sin fallback.
    // Si user-service no responde, task-service se bloquea indefinidamente y TODAS las operaciones fallan con 500.
    // Resilience4j está como dependencia en pom.xml pero no se usa aquí.
    @Retry(name = "userService")
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserName")
    public String getUserName(Long userId) {
        String url = userServiceUrl + "/api/users/" + userId + "/name";
        // TODO: [LABORATORIO - BUG 3] - Sin manejo de excepciones: si la llamada falla, la excepción se propaga y rompe toda la operación del task-service.
        Map<String, String> response = restTemplate.getForObject(url, Map.class);
        return response != null ? response.get("name") : "Unknown";
    }

    public String fallbackGetUserName(Long id, Throwable t){
        System.out.println("CircuitBreaker OPEN - Servicio user-service caido...." + "Error:" + t.getMessage());
        return "Unknown User";
    }

    // Obtiene nombres de múltiples usuarios en una sola llamada
    public Map<Long, String> getUserNames(List<Long> userIds) {
        Map<Long, String> names = new HashMap<>();
        for (Long userId : userIds) {
            names.put(userId, getUserName(userId)); // Reutiliza el circuit breaker
        }
    return names;
    }


}
