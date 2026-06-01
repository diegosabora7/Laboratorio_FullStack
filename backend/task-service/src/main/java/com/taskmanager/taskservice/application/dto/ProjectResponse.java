package com.taskmanager.taskservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
