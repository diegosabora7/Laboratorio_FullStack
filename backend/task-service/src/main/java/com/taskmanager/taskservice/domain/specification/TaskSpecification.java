package com.taskmanager.taskservice.domain.specification;

import com.taskmanager.taskservice.domain.model.Task;
import com.taskmanager.taskservice.domain.model.TaskPriority;
import com.taskmanager.taskservice.domain.model.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

// CÓDIGO ANTERIOR (placeholder):
// public class TaskSpecification {
//     // TODO: Implementar especificaciones de búsqueda
// }

// FEATURE 8: Specifications para búsqueda avanzada con filtros dinámicos
public class TaskSpecification {

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasAssignee(Long assigneeId) {
        return (root, query, cb) -> assigneeId == null ? null : cb.equal(root.get("assigneeId"), assigneeId);
    }

    public static Specification<Task> hasProject(Long projectId) {
        return (root, query, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> titleOrDescriptionContains(String query) {
        return (root, criteriaQuery, cb) -> {
            if (query == null || query.isBlank()) return null;
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Task> dueBefore(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.lessThan(root.get("dueDate"), date);
    }

    public static Specification<Task> isOverdue() {
        return (root, query, cb) -> cb.and(
                cb.lessThan(root.get("dueDate"), LocalDate.now()),
                cb.notEqual(root.get("status"), TaskStatus.DONE)
        );
    }
}
