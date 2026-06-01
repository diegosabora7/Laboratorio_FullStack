package com.taskmanager.taskservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    private String status;
}
