package com.taskmaster.taskmaster.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps the open SSE (Server-Sent Events) connections and pushes notifications to them.
 *
 * - One user can have several open streams (e.g. two browser tabs), so each user maps to a list.
 * - Notifications are sent only AFTER the database transaction commits, so users are never
 *   told about a change that was rolled back.
 * - A heartbeat every 25 seconds keeps idle connections open and detects closed clients.
 * - A stream ends when the user's token expires (max 30 minutes); the client then reconnects.
 *
 * Note: connections are held in memory, so this works for a single application instance.
 * Running several instances would need a shared broker (e.g. Redis Pub/Sub).
 */
@Slf4j
@Service
public class NotificationService {

    private static final Duration MAX_STREAM_DURATION = Duration.ofMinutes(30);

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Opens a new stream for the user. */
    public SseEmitter subscribe(Long userId, Instant tokenExpiresAt) {
        long untilTokenExpiry = Duration.between(Instant.now(), tokenExpiresAt).toMillis();
        long timeout = Math.max(1_000, Math.min(untilTokenExpiry, MAX_STREAM_DURATION.toMillis()));

        SseEmitter emitter = new SseEmitter(timeout);

        emitters.compute(userId, (id, list) -> {
            List<SseEmitter> result = (list != null) ? list : new CopyOnWriteArrayList<>();
            result.add(emitter);
            return result;
        });

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(emitter::complete);              // completion callback then removes it
        emitter.onError(ex -> remove(userId, emitter));

        send(userId, emitter, SseEmitter.event()
                .name("connected")
                .data(Map.of("message", "Connected to TaskMaster notifications")));

        log.debug("User {} subscribed to notifications ({} open stream(s))", userId, emitters.get(userId).size());
        return emitter;
    }

    /** Delivers a notification to every open stream of each recipient, after commit. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationEvent event) {
        log.debug("Notification {} for users {}", event.message().type(), event.recipientIds());

        for (Long userId : event.recipientIds()) {
            List<SseEmitter> userEmitters = emitters.get(userId);
            if (userEmitters == null) {
                log.debug("User {} has no open stream; {} not delivered", userId, event.message().type());
                continue;   // user is not connected right now
            }
            for (SseEmitter emitter : userEmitters) {
                send(userId, emitter, SseEmitter.event()
                        .id(event.message().id())
                        .name(event.message().type().name())
                        .data(event.message()));
            }
        }
    }

    /** SSE comment line (": heartbeat") — ignored by clients, keeps the connection alive. */
    @Scheduled(fixedRate = 25_000)
    public void sendHeartbeat() {
        emitters.forEach((userId, userEmitters) ->
                userEmitters.forEach(emitter -> send(userId, emitter, SseEmitter.event().comment("heartbeat"))));
    }

    private void send(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException ex) {
            // Client disconnected; the servlet container cleans up the connection
            log.debug("Stream of user {} is closed: {}", userId, ex.getMessage());
            remove(userId, emitter);
        } catch (Exception ex) {
            log.warn("Failed to send notification to user {}", userId, ex);
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}