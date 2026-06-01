package com.taskmanager.taskservice.application.service;

import com.taskmanager.taskservice.application.dto.TaskRequest;
import com.taskmanager.taskservice.application.dto.TaskResponse;
import com.taskmanager.taskservice.domain.model.Project;
import com.taskmanager.taskservice.domain.model.Task;
import com.taskmanager.taskservice.domain.model.TaskPriority;
import com.taskmanager.taskservice.domain.model.TaskStatus;
import com.taskmanager.taskservice.domain.repository.ProjectRepository;
import com.taskmanager.taskservice.domain.repository.TaskRepository;
import com.taskmanager.taskservice.infrastructure.client.UserServiceClient;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserServiceClient userServiceClient;

    @Cacheable(value = "tasks", key = "#id")
    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        return toResponse(task);
    }

    // TODO: [LABORATORIO - BUG 1] - N+1 queries: se cargan TODAS las tareas con
    // findAll() y se filtran en memoria.
    // Además, toResponse() hace una llamada HTTP individual al user-service por
    // cada tarea (N+1 al servicio externo).
    @Cacheable(value = "tasksByProject", key = "#projectId")
    @Transactional(readOnly = true)
    public List<TaskResponse> getByProject(Long projectId) {
        // System.out.println("🚨 [BUG 1] Ejecutando findAll() - cargando TODAS las
        // tareas de TODOS los proyectos...");
        // Aqui coloco el llamado al nuevo metodo optimizado y no findAll()
        List<Task> allTasks = taskRepository.findByProjectIdOptimized(projectId);
        // System.out.println("🚨 [BUG 1] Total tareas cargadas en memoria: " +
        // allTasks.size() + " (solo necesitamos las del proyecto " + projectId + ")");

        // SOLUCION BUG 1
        // Obtener IDs únicos de asignados (evita llamadas duplicadas)
        Map<Long, String> userNames = allTasks.stream()
                .map(Task::getAssigneeId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> userServiceClient.getUserName(id)));

        List<TaskResponse> result = allTasks.stream()
                .map(task -> toResponseWithName(task, userNames.getOrDefault(task.getAssigneeId(), null)))
                .collect(Collectors.toList());

        return result;
    }

    public Page<TaskResponse> getByAssignee(Long userId, Pageable pageable) {
        return taskRepository.findByAssigneeId(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    @CacheEvict(value = { "tasks", "tasksByProject" }, allEntries = true)
    public TaskResponse create(TaskRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + request.getProjectId()));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .priority(TaskPriority.valueOf(request.getPriority()))
                .project(project)
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .build();

        // TODO: [LABORATORIO - BUG 4] - Cache stale: se crea una tarea pero no se
        // invalida la caché de tasksByProject ni tasks.
        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    // TODO: [LABORATORIO - BUG 4] - Cache stale: falta @CacheEvict. Los datos en
    // caché quedan desactualizados hasta que expire el TTL (~5 min).
    // TODO: [LABORATORIO - BUG 2] - Race condition: no se usa optimistic locking.
    // Dos usuarios pueden actualizar simultáneamente y uno pierde su cambio.
    @Transactional
    @CacheEvict(value = { "tasks", "tasksByProject" }, allEntries = true)
    public TaskResponse updateStatus(Long id, String status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        // TODO: [LABORATORIO - BUG 2] - Race condition: no hay verificación de versión.
        // El último en escribir gana silenciosamente.
        task.setStatus(TaskStatus.valueOf(status));
        Task updated = taskRepository.save(task);
        return toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = { "tasks", "tasksByProject" }, allEntries = true)
    public TaskResponse assign(Long id, Long assigneeId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setAssigneeId(assigneeId);
        Task updated = taskRepository.save(task);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks(Long projectId) {
        List<Task> overdueTasks = taskRepository.findByProjectIdAndDueDateBeforeAndStatusNot(
                projectId, LocalDate.now(), TaskStatus.DONE);
        return overdueTasks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProjectStats(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        System.out.println("[STATS] Tareas encontradas para proyecto " + projectId + ": " + tasks.size());
        long totalTasks = tasks.size();
        long todoCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long inReviewCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_REVIEW).count();
        long doneCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        double completionPercentage = totalTasks > 0 ? (doneCount * 100.0 / totalTasks) : 0;

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("projectId", projectId);
        stats.put("totalTasks", totalTasks);
        stats.put("todoCount", todoCount);
        stats.put("inProgressCount", inProgressCount);
        stats.put("inReviewCount", inReviewCount);
        stats.put("doneCount", doneCount);
        stats.put("completionPercentage", completionPercentage);
        System.out.println("[STATS] Retornando: " + stats);
        return stats;
    }

    // TODO: [LABORATORIO - BUG 1] - N+1 queries: cada llamada a toResponse()
    // dispara una petición HTTP al user-service para obtener el nombre del
    // asignado.
    private TaskResponse toResponseWithName(Task task, String assigneeName) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .assigneeId(task.getAssigneeId())
                .assigneeName(assigneeName)
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // TODO: [LABORATORIO - BUG 3] - Fallo en cascada: si user-service está caído,
    // esta llamada bloquea y falla toda la operación.
    private TaskResponse toResponse(Task task) {
        String assigneeName = null;
        if (task.getAssigneeId() != null) {
            assigneeName = userServiceClient.getUserName(task.getAssigneeId());
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .assigneeId(task.getAssigneeId())
                .assigneeName(assigneeName)
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
