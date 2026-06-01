package com.taskmanager.userservice.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class TaskServiceClient {

    private final RestTemplate restTemplate;

    @Value("${task-service.url}")
    private String taskServiceUrl;

    // TODO: [LABORATORIO - BUG 3] - Sin timeout, sin circuit breaker, sin fallback.
    public Object getTasksByUser(Long userId) {
        String url = taskServiceUrl + "/api/tasks/assignee/" + userId;
        return restTemplate.getForObject(url, Object.class);
    }
}
