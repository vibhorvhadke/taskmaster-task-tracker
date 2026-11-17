package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.invitation.InvitationResponse;
import com.taskmaster.taskmaster.dto.invitation.InviteMemberRequest;
import com.taskmaster.taskmaster.entity.TeamInvitation;
import com.taskmaster.taskmaster.entity.TeamMember;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.enums.InvitationStatus;
import com.taskmaster.taskmaster.enums.TeamRole;
import com.taskmaster.taskmaster.exception.BadRequestException;
import com.taskmaster.taskmaster.exception.ConflictException;
import com.taskmaster.taskmaster.exception.ForbiddenException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.notification.NotificationPublisher;
import com.taskmaster.taskmaster.repository.TeamInvitationRepository;
import com.taskmaster.taskmaster.repository.TeamMemberRepository;
import com.taskmaster.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Inviting users to teams, and accepting or declining invitations.
 */
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final TeamInvitationRepository invitationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamService teamService;
    private final NotificationPublisher notificationPublisher;

    /**
     * The team OWNER invites a registered user by email.
     */
    @Transactional
    public InvitationResponse invite(Long teamId, Long ownerId, InviteMemberRequest request) {
        TeamMember ownerMembership = teamService.requireOwner(teamId, ownerId);

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User invitedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No registered user found with email: " + email));

        if (invitedUser.getId().equals(ownerId)) {
            throw new BadRequestException("You cannot invite yourself");
        }
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, invitedUser.getId())) {
            throw new ConflictException("User is already a member of this team");
        }
        if (invitationRepository.existsByTeamIdAndInvitedUserIdAndStatus(
                teamId, invitedUser.getId(), InvitationStatus.PENDING)) {
            throw new ConflictException("A pending invitation already exists for this user");
        }

        TeamInvitation invitation = invitationRepository.save(TeamInvitation.builder()
                .team(ownerMembership.getTeam())
                .invitedUser(invitedUser)
                .invitedBy(ownerMembership.getUser())
                .status(InvitationStatus.PENDING)
                .build());

        notificationPublisher.invitationReceived(invitation);
        return InvitationResponse.from(invitation);
    }

    /**
     * Pending invitations received by the logged-in user.
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyPendingInvitations(Long userId) {
        return invitationRepository.findByInvitedUserIdAndStatus(userId, InvitationStatus.PENDING).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    /**
     * Accepts an invitation and adds the user to the team as MEMBER, in one transaction.
     */
    @Transactional
    public InvitationResponse accept(Long invitationId, Long userId) {
        TeamInvitation invitation = findPendingInvitationFor(invitationId, userId);

        Long teamId = invitation.getTeam().getId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            teamMemberRepository.save(TeamMember.builder()
                    .team(invitation.getTeam())
                    .user(invitation.getInvitedUser())
                    .role(TeamRole.MEMBER)
                    .build());
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        return InvitationResponse.from(invitation);
    }

    @Transactional
    public InvitationResponse decline(Long invitationId, Long userId) {
        TeamInvitation invitation = findPendingInvitationFor(invitationId, userId);
        invitation.setStatus(InvitationStatus.DECLINED);
        return InvitationResponse.from(invitation);
    }

    /**
     * Loads an invitation and checks that it belongs to this user and is still PENDING.
     */
    private TeamInvitation findPendingInvitationFor(Long invitationId, Long userId) {
        TeamInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));

        if (!invitation.getInvitedUser().getId().equals(userId)) {
            throw new ForbiddenException("This invitation was not sent to you");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException(
                    "Invitation has already been " + invitation.getStatus().name().toLowerCase(Locale.ROOT));
        }
        return invitation;
    }
}