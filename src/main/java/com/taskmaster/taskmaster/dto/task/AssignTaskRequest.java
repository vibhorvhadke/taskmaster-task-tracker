package com.taskmaster.taskmaster.dto.task;

/**
 * Request body for PATCH /api/tasks/{taskId}/assignee.
 * Example: { "assigneeId": 2 }   or   { "assigneeId": null } to unassign.
 */
public record AssignTaskRequest(
        Long assigneeId
) {
}