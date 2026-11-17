package com.taskmaster.taskmaster.dto.task;

import com.taskmaster.taskmaster.enums.TaskPriority;
import com.taskmaster.taskmaster.enums.TaskScope;
import com.taskmaster.taskmaster.enums.TaskStatus;

/**
 * Filter criteria for GET /api/tasks. Every field except scope is optional (null = no filter).
 */
public record TaskFilter(
        TaskScope scope,
        Long teamId,
        TaskStatus status,
        TaskPriority priority,
        String search
) {
}