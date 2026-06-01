package com.taskmanager.taskservice.domain.repository;

import com.taskmanager.taskservice.domain.model.Task;
import com.taskmanager.taskservice.domain.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByProjectId(Long projectId);

    @Query("SELECT t FROM Task t JOIN FETCH t.project WHERE t.project.id = :projectId")
    List<Task> findByProjectIdOptimized(@Param("projectId") Long projectId);

    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    List<Task> findByProjectIdAndDueDateBeforeAndStatusNot(Long projectId, LocalDate date, TaskStatus status);
}
