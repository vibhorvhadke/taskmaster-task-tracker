package com.taskmaster.taskmaster.notification;

import com.taskmaster.taskmaster.dto.notification.NotificationMessage;

import java.util.Set;

/**
 * Internal Spring application event: "send this message to these users".
 * Published inside a transaction; delivered only after that transaction commits.
 */
public record NotificationEvent(
        Set<Long> recipientIds,
        NotificationMessage message
) {
}