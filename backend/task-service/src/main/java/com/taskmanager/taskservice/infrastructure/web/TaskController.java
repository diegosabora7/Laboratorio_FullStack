package com.taskmanager.taskservice.infrastructure.web;

import com.taskmanager.taskservice.application.dto.TaskRequest;
import com.taskmanager.taskservice.application.dto.TaskResponse;
import com.taskmanager.taskservice.application.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getByProject(projectId));
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<Page<TaskResponse>> getByAssignee(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(taskService.getByAssignee(userId, pageable));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TaskResponse> assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        return ResponseEntity.ok(taskService.assign(id, assigneeId));
    }

    // CÓDIGO ANTERIOR (placeholder):
    // @GetMapping("/search")
    // public ResponseEntity<List<TaskResponse>> search(...) { return ResponseEntity.ok(List.of()); }

    // FEATURE 8: Búsqueda avanzada con filtros dinámicos, paginado y sorteable
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponse>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dueBefore,
            @RequestParam(required = false) Boolean overdue,
            Pageable pageable) {
        return ResponseEntity.ok(taskService.search(status, priority, assigneeId, projectId, query, dueBefore, overdue, pageable));
    }

    // Endpoint de estadísticas del proyecto - devuelve conteo de tareas por estado
    @GetMapping("/project/{projectId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getProjectStats(projectId));
    }

    // TODO: [FEATURE - OVERDUE] Implementar tareas vencidas
    // Debe retornar tareas cuya fecha límite ha pasado y no están en estado DONE
    @GetMapping("/project/{projectId}/overdue")
    public ResponseEntity<List<TaskResponse>> getOverdue(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getOverdueTasks(projectId));
    }

}
