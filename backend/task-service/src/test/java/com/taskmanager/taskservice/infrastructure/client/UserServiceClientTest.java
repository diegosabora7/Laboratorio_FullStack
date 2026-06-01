package com.taskmanager.taskservice.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userServiceClient, "userServiceUrl", "http://localhost:8082");
    }

    // =========================================================================
    // BUG 3 — Verifica comportamiento del client con y sin fallo
    // =========================================================================

    @Test
    @DisplayName("getUserName debe retornar el nombre cuando user-service responde correctamente")
    void getUserName_returnsName_whenServiceResponds() {
        Map<String, String> response = Map.of("name", "Diego Yandun");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        String result = userServiceClient.getUserName(1L);

        assertThat(result).isEqualTo("Diego Yandun");
    }

    @Test
    @DisplayName("getUserName debe retornar 'Unknown' cuando la respuesta es null")
    void getUserName_returnsUnknown_whenResponseIsNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        String result = userServiceClient.getUserName(1L);

        assertThat(result).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("fallbackGetUserName debe retornar 'Unknown User' cuando el servicio falla")
    void fallbackGetUserName_returnsUnknownUser() {
        String result = userServiceClient.fallbackGetUserName(1L, new ResourceAccessException("Connection refused"));

        assertThat(result).isEqualTo("Unknown User");
    }

    @Test
    @DisplayName("getUserNames batch debe retornar mapa con nombres para múltiples IDs")
    void getUserNames_returnsBatchResults() {
        when(restTemplate.getForObject(
                eq("http://localhost:8082/api/users/1/name"), eq(Map.class)))
                .thenReturn(Map.of("name", "Diego"));
        when(restTemplate.getForObject(
                eq("http://localhost:8082/api/users/2/name"), eq(Map.class)))
                .thenReturn(Map.of("name", "Admin"));

        Map<Long, String> result = userServiceClient.getUserNames(java.util.List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo("Diego");
        assertThat(result.get(2L)).isEqualTo("Admin");
    }
}
