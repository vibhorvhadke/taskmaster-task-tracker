package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.notification.NotificationService;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Real-time notifications using SSE (Server-Sent Events). Requires a valid token.
 *
 * GET /api/notifications/stream  -> keeps the connection open (Content-Type: text/event-stream)
 *                                   and pushes events as they happen.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return notificationService.subscribe(currentUser.id(), currentUser.tokenExpiresAt());
    }
}