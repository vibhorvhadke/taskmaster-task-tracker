package com.taskmaster.taskmaster.notification;

import com.taskmaster.taskmaster.dto.notification.NotificationMessage;
import com.taskmaster.taskmaster.dto.user.UserSummary;
import com.taskmaster.taskmaster.entity.Task;
import com.taskmaster.taskmaster.entity.TeamInvitation;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.enums.NotificationType;
import com.taskmaster.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Decides WHO gets notified about WHAT, and publishes a NotificationEvent.
 * Called by the services; the person who made the change is never notified.
 *
 * Recipients:
 *  - TASK_ASSIGNED        : the new assignee
 *  - TASK_UPDATED         : assignee + creator
 *  - TASK_STATUS_CHANGED  : assignee + creator
 *  - COMMENT_ADDED        : assignee + creator
 *  - INVITATION_RECEIVED  : the invited user
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    public void taskAssigned(Task task, Long actorId) {
        if (task.getAssignee() == null) {
            return;
        }
        publishForTask(task, actorId, Set.of(task.getAssignee().getId()),
                NotificationType.TASK_ASSIGNED, "%s assigned you the task \"%s\"");
    }

    public void taskUpdated(Task task, Long actorId) {
        publishForTask(task, actorId, assigneeAndCreator(task),
                NotificationType.TASK_UPDATED, "%s updated the task \"%s\"");
    }

    public void taskStatusChanged(Task task, Long actorId) {
        publishForTask(task, actorId, assigneeAndCreator(task),
                NotificationType.TASK_STATUS_CHANGED,
                "%s changed the status of \"%s\" to " + task.getStatus());
    }

    public void commentAdded(Task task, Long actorId) {
        publishForTask(task, actorId, assigneeAndCreator(task),
                NotificationType.COMMENT_ADDED, "%s commented on \"%s\"");
    }

    public void invitationReceived(TeamInvitation invitation) {
        User actor = invitation.getInvitedBy();
        String text = "%s invited you to join the team \"%s\""
                .formatted(actor.getFullName(), invitation.getTeam().getName());

        publish(Set.of(invitation.getInvitedUser().getId()), actor.getId(), new NotificationMessage(
                UUID.randomUUID().toString(),
                NotificationType.INVITATION_RECEIVED,
                text,
                null,
                invitation.getTeam().getId(),
                UserSummary.from(actor),
                Instant.now()
        ));
    }

    // ---------------------------------------------------------------------

    /** template has two %s placeholders: actor name, task title. */
    private void publishForTask(Task task, Long actorId, Set<Long> recipients,
                                NotificationType type, String template) {
        User actor = userRepository.findById(actorId).orElse(null);
        String actorName = actor != null ? actor.getFullName() : "Someone";

        publish(recipients, actorId, new NotificationMessage(
                UUID.randomUUID().toString(),
                type,
                template.formatted(actorName, task.getTitle()),
                task.getId(),
                task.getTeam() != null ? task.getTeam().getId() : null,
                UserSummary.from(actor),
                Instant.now()
        ));
    }

    private void publish(Set<Long> recipients, Long actorId, NotificationMessage message) {
        Set<Long> targets = new HashSet<>(recipients);
        targets.remove(actorId);          // never notify the person who made the change
        if (!targets.isEmpty()) {
            eventPublisher.publishEvent(new NotificationEvent(Set.copyOf(targets), message));
        }
    }

    private Set<Long> assigneeAndCreator(Task task) {
        Set<Long> ids = new HashSet<>();
        ids.add(task.getCreatedBy().getId());
        if (task.getAssignee() != null) {
            ids.add(task.getAssignee().getId());
        }
        return ids;
    }
}