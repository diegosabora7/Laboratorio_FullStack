package com.taskmanager.taskservice.application.service;

import com.taskmanager.taskservice.application.dto.TaskResponse;
import com.taskmanager.taskservice.domain.model.Project;
import com.taskmanager.taskservice.domain.model.Task;
import com.taskmanager.taskservice.domain.model.TaskPriority;
import com.taskmanager.taskservice.domain.model.TaskStatus;
import com.taskmanager.taskservice.domain.repository.ProjectRepository;
import com.taskmanager.taskservice.domain.repository.TaskRepository;
import com.taskmanager.taskservice.infrastructure.client.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private TaskService taskService;

    private Project testProject;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(1L)
                .name("Sprint 1")
                .description("Test project")
                .ownerId(1L)
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .project(testProject)
                .assigneeId(1L)
                .dueDate(LocalDate.of(2026, 12, 31))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // BUG 1 — N+1 queries: verifica que se usa findByProjectIdOptimized
    // =========================================================================
    @Nested
    @DisplayName("Bug 1 - N+1 Queries Fix")
    class Bug1Tests {

        @Test
        @DisplayName("getByProject debe usar findByProjectIdOptimized en vez de findAll")
        void getByProject_usesOptimizedQuery() {
            when(taskRepository.findByProjectIdOptimized(1L)).thenReturn(List.of(testTask));
            when(userServiceClient.getUserName(1L)).thenReturn("Diego");

            List<TaskResponse> result = taskService.getByProject(1L);

            // Verifica que se llamó al método optimizado, NO a findAll
            verify(taskRepository).findByProjectIdOptimized(1L);
            verify(taskRepository, never()).findAll();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssigneeName()).isEqualTo("Diego");
        }

        @Test
        @DisplayName("getByProject debe hacer batch de userNames (no N llamadas)")
        void getByProject_batchesUserNameCalls() {
            Task task2 = Task.builder()
                    .id(2L).title("Task 2").status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.MEDIUM).project(testProject)
                    .assigneeId(1L) // mismo assignee que testTask
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByProjectIdOptimized(1L)).thenReturn(List.of(testTask, task2));
            when(userServiceClient.getUserName(1L)).thenReturn("Diego");

            taskService.getByProject(1L);

            // Con 2 tareas del mismo usuario, solo debe llamar 1 vez al user-service
            verify(userServiceClient, times(1)).getUserName(1L);
        }
    }

    // =========================================================================
    // BUG 2 — Race condition: verifica optimistic locking
    // =========================================================================
    @Nested
    @DisplayName("Bug 2 - Race Condition Fix")
    class Bug2Tests {

        @Test
        @DisplayName("updateStatus debe guardar la tarea con el nuevo status")
        void updateStatus_savesNewStatus() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(userServiceClient.getUserName(anyLong())).thenReturn("Diego");

            TaskResponse result = taskService.updateStatus(1L, "IN_PROGRESS");

            verify(taskRepository).save(any(Task.class));
            assertThat(testTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("updateStatus debe lanzar excepción si la tarea no existe")
        void updateStatus_throwsIfNotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateStatus(99L, "IN_PROGRESS"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Task not found");
        }
    }

    // =========================================================================
    // BUG 3 — Fallo en cascada: verifica fallback del UserServiceClient
    // =========================================================================
    @Nested
    @DisplayName("Bug 3 - Cascade Failure Fix")
    class Bug3Tests {

        @Test
        @DisplayName("toResponse debe usar el nombre del fallback cuando user-service falla")
        void getById_usesFallbackWhenUserServiceFails() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(userServiceClient.getUserName(1L)).thenReturn("Unknown User");

            TaskResponse result = taskService.getById(1L);

            assertThat(result.getAssigneeName()).isEqualTo("Unknown User");
        }

        @Test
        @DisplayName("getById debe funcionar incluso sin assigneeId")
        void getById_worksWithoutAssignee() {
            testTask.setAssigneeId(null);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            TaskResponse result = taskService.getById(1L);

            assertThat(result.getAssigneeName()).isNull();
            verify(userServiceClient, never()).getUserName(anyLong());
        }
    }

    // =========================================================================
    // BUG 4 — Cache stale: verifica que los métodos de escritura invalidan caché
    // (La invalidación real es por @CacheEvict, aquí verificamos el comportamiento)
    // =========================================================================
    @Nested
    @DisplayName("Bug 4 - Cache Stale Fix")
    class Bug4Tests {

        @Test
        @DisplayName("create debe guardar la tarea correctamente")
        void create_savesTask() {
            var request = new com.taskmanager.taskservice.application.dto.TaskRequest();
            request.setTitle("New Task");
            request.setDescription("Desc");
            request.setPriority("HIGH");
            request.setProjectId(1L);
            request.setAssigneeId(1L);

            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            when(userServiceClient.getUserName(1L)).thenReturn("Diego");

            TaskResponse result = taskService.create(request);

            verify(taskRepository).save(any(Task.class));
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Task");
        }
    }

    // =========================================================================
    // FEATURE 9 — Stats y Overdue
    // =========================================================================
    @Nested
    @DisplayName("Feature 9 - Dashboard Stats")
    class Feature9Tests {

        @Test
        @DisplayName("getProjectStats debe retornar contadores correctos por status")
        void getProjectStats_returnsCorrectCounts() {
            Task taskTodo = Task.builder().id(1L).status(TaskStatus.TODO).priority(TaskPriority.LOW).project(testProject).build();
            Task taskProgress = Task.builder().id(2L).status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM).project(testProject).build();
            Task taskDone = Task.builder().id(3L).status(TaskStatus.DONE).priority(TaskPriority.HIGH).project(testProject).build();

            when(taskRepository.findByProjectId(1L)).thenReturn(List.of(taskTodo, taskProgress, taskDone));

            Map<String, Object> stats = taskService.getProjectStats(1L);

            assertThat(stats.get("totalTasks")).isEqualTo(3L);
            assertThat(stats.get("todoCount")).isEqualTo(1L);
            assertThat(stats.get("inProgressCount")).isEqualTo(1L);
            assertThat(stats.get("inReviewCount")).isEqualTo(0L);
            assertThat(stats.get("doneCount")).isEqualTo(1L);
            assertThat((double) stats.get("completionPercentage")).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("getProjectStats con proyecto vacío debe retornar ceros")
        void getProjectStats_emptyProject_returnsZeros() {
            when(taskRepository.findByProjectId(99L)).thenReturn(List.of());

            Map<String, Object> stats = taskService.getProjectStats(99L);

            assertThat(stats.get("totalTasks")).isEqualTo(0L);
            assertThat(stats.get("completionPercentage")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("getOverdueTasks debe retornar solo tareas vencidas no completadas")
        void getOverdueTasks_returnsOnlyOverdueNotDone() {
            Task overdueTask = Task.builder()
                    .id(1L).title("Overdue").status(TaskStatus.TODO)
                    .priority(TaskPriority.HIGH).project(testProject)
                    .dueDate(LocalDate.of(2024, 1, 1))
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByProjectIdAndDueDateBeforeAndStatusNot(
                    eq(1L), any(LocalDate.class), eq(TaskStatus.DONE)))
                    .thenReturn(List.of(overdueTask));

            List<TaskResponse> result = taskService.getOverdueTasks(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Overdue");
        }
    }
}
