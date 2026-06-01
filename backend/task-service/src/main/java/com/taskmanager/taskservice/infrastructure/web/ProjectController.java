package com.taskmanager.taskservice.infrastructure.web;

import com.taskmanager.taskservice.application.dto.ProjectRequest;
import com.taskmanager.taskservice.application.dto.ProjectResponse;
import com.taskmanager.taskservice.application.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll() {
        return ResponseEntity.ok(projectService.getAll());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ProjectResponse>> getByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(projectService.getByOwner(ownerId));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }
}
