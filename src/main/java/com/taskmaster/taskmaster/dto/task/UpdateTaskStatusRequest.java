package com.taskmaster.taskmaster.dto.task;

import com.taskmaster.taskmaster.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PATCH /api/tasks/{taskId}/status.
 * Example: { "status": "COMPLETED" }
 */
public record UpdateTaskStatusRequest(

        @NotNull(message = "Status is required (OPEN, IN_PROGRESS or COMPLETED)")
        TaskStatus status
) {
}