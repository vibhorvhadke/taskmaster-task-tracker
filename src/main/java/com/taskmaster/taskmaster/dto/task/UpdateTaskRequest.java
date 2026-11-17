package com.taskmaster.taskmaster.dto.task;

import com.taskmaster.taskmaster.enums.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for PATCH /api/tasks/{taskId}.
 * Every field is optional: null means "leave unchanged".
 */
public record UpdateTaskRequest(

        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate,

        TaskPriority priority
) {
}