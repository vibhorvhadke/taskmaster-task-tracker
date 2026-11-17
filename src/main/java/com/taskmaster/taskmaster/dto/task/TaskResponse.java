package com.taskmaster.taskmaster.dto.task;

import com.taskmaster.taskmaster.dto.user.UserSummary;
import com.taskmaster.taskmaster.entity.Task;
import com.taskmaster.taskmaster.enums.TaskPriority;
import com.taskmaster.taskmaster.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Task details returned by the API.
 * teamId and teamName are null for personal tasks; assignee is null for unassigned tasks.
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        TaskStatus status,
        TaskPriority priority,
        Long teamId,
        String teamName,
        UserSummary createdBy,
        UserSummary assignee,
        Instant createdAt,
        Instant updatedAt
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getStatus(),
                task.getPriority(),
                task.getTeam() != null ? task.getTeam().getId() : null,
                task.getTeam() != null ? task.getTeam().getName() : null,
                UserSummary.from(task.getCreatedBy()),
                UserSummary.from(task.getAssignee()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}