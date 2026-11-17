package com.taskmaster.taskmaster.dto.task;

import com.taskmaster.taskmaster.enums.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for POST /api/tasks.
 *
 * teamId     : optional. Omit for a personal task.
 * assigneeId : optional. For team tasks it must be a team member;
 *              personal tasks are always assigned to the creator.
 * priority   : optional. Defaults to MEDIUM.
 */
public record CreateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate,

        TaskPriority priority,

        Long teamId,

        Long assigneeId
) {
}