package com.taskmaster.taskmaster.dto.notification;

import com.taskmaster.taskmaster.dto.user.UserSummary;
import com.taskmaster.taskmaster.enums.NotificationType;

import java.time.Instant;

/**
 * A notification pushed to the client over the SSE stream.
 *
 * Example event:
 *   id: 6f1c...
 *   event: TASK_ASSIGNED
 *   data: {"id":"6f1c...","type":"TASK_ASSIGNED","message":"Priya Sharma assigned you the task \"Fix navbar\"",
 *          "taskId":4,"teamId":1,"actor":{"id":1,"fullName":"Priya Sharma","email":"priya@example.com"},
 *          "createdAt":"2026-11-16T11:30:00Z"}
 */
public record NotificationMessage(
        String id,
        NotificationType type,
        String message,
        Long taskId,
        Long teamId,
        UserSummary actor,
        Instant createdAt
) {
}