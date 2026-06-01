package com.taskmanager.taskservice.application.service;

import com.taskmanager.taskservice.application.dto.ProjectRequest;
import com.taskmanager.taskservice.application.dto.ProjectResponse;
import com.taskmanager.taskservice.domain.model.Project;
import com.taskmanager.taskservice.domain.model.ProjectStatus;
import com.taskmanager.taskservice.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectResponse getById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return toResponse(project);
    }

    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProjectResponse> getByOwner(Long ownerId) {
        return projectRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        ProjectStatus status = ProjectStatus.ACTIVE;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            status = ProjectStatus.valueOf(request.getStatus());
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(request.getOwnerId())
                .status(status)
                .build();

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwnerId())
                .status(project.getStatus() != null ? project.getStatus().name() : null)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
