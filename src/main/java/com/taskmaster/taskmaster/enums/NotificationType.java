package com.taskmaster.taskmaster.enums;

/** Kinds of real-time notifications sent over the SSE stream. */
public enum NotificationType {
    TASK_ASSIGNED,
    TASK_UPDATED,
    TASK_STATUS_CHANGED,
    COMMENT_ADDED,
    INVITATION_RECEIVED
}