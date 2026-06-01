package com.taskmanager.taskservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long projectId;
    private String projectName;
    private Long assigneeId;
    private String assigneeName;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
