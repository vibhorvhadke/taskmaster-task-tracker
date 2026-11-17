package com.taskmaster.taskmaster.dto.invitation;

import com.taskmaster.taskmaster.dto.user.UserSummary;
import com.taskmaster.taskmaster.entity.TeamInvitation;
import com.taskmaster.taskmaster.enums.InvitationStatus;

import java.time.Instant;

/**
 * Details of a team invitation.
 */
public record InvitationResponse(
        Long id,
        Long teamId,
        String teamName,
        UserSummary invitedUser,
        UserSummary invitedBy,
        InvitationStatus status,
        Instant createdAt
) {

    public static InvitationResponse from(TeamInvitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getTeam().getId(),
                invitation.getTeam().getName(),
                UserSummary.from(invitation.getInvitedUser()),
                UserSummary.from(invitation.getInvitedBy()),
                invitation.getStatus(),
                invitation.getCreatedAt()
        );
    }
}