package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.invitation.InvitationResponse;
import com.taskmaster.taskmaster.dto.invitation.InviteMemberRequest;
import com.taskmaster.taskmaster.entity.Team;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the invitation rules: who can invite whom, and who can accept or decline.
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final Long TEAM_ID = 10L;
    private static final Long INVITATION_ID = 50L;

    @Mock private TeamInvitationRepository invitationRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamService teamService;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks private InvitationService invitationService;

    private final User priya = User.builder().id(1L).fullName("Priya Sharma").email("priya@example.com").build();
    private final User amit = User.builder().id(2L).fullName("Amit Verma").email("amit@example.com").build();
    private final User rahul = User.builder().id(3L).fullName("Rahul Mehta").email("rahul@example.com").build();
    private final Team team = Team.builder().id(TEAM_ID).name("Website Launch").createdBy(priya).build();
    private final TeamMember ownerMembership =
            TeamMember.builder().team(team).user(priya).role(TeamRole.OWNER).build();

    @Test
    @DisplayName("owner invites a registered user; email is matched case-insensitively and the user is notified")
    void ownerInvitesUser() {
        when(teamService.requireOwner(TEAM_ID, 1L)).thenReturn(ownerMembership);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(rahul));
        when(invitationRepository.save(any(TeamInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationResponse response = invitationService.invite(TEAM_ID, 1L,
                new InviteMemberRequest("  Rahul@Example.COM "));

        assertThat(response.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.invitedUser().id()).isEqualTo(3L);
        assertThat(response.invitedBy().id()).isEqualTo(1L);
        verify(notificationPublisher).invitationReceived(any(TeamInvitation.class));
    }

    @Test
    @DisplayName("an owner cannot invite themselves")
    void cannotInviteSelf() {
        when(teamService.requireOwner(TEAM_ID, 1L)).thenReturn(ownerMembership);
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(priya));

        assertThatThrownBy(() -> invitationService.invite(TEAM_ID, 1L, new InviteMemberRequest("priya@example.com")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You cannot invite yourself");
    }

    @Test
    @DisplayName("inviting an unknown email gives 404")
    void unknownEmail() {
        when(teamService.requireOwner(TEAM_ID, 1L)).thenReturn(ownerMembership);
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.invite(TEAM_ID, 1L, new InviteMemberRequest("nobody@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("inviting an existing member gives 409")
    void alreadyMember() {
        when(teamService.requireOwner(TEAM_ID, 1L)).thenReturn(ownerMembership);
        when(userRepository.findByEmail("amit@example.com")).thenReturn(Optional.of(amit));
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, 2L)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.invite(TEAM_ID, 1L, new InviteMemberRequest("amit@example.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    @DisplayName("a second pending invitation for the same user gives 409")
    void duplicatePendingInvitation() {
        when(teamService.requireOwner(TEAM_ID, 1L)).thenReturn(ownerMembership);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(rahul));
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, 3L)).thenReturn(false);
        when(invitationRepository.existsByTeamIdAndInvitedUserIdAndStatus(TEAM_ID, 3L, InvitationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> invitationService.invite(TEAM_ID, 1L, new InviteMemberRequest("rahul@example.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pending invitation already exists");
    }

    @Test
    @DisplayName("accepting adds the user to the team as MEMBER")
    void acceptAddsMember() {
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitationFor(rahul)));
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, 3L)).thenReturn(false);

        InvitationResponse response = invitationService.accept(INVITATION_ID, 3L);

        ArgumentCaptor<TeamMember> saved = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(rahul);
        assertThat(saved.getValue().getRole()).isEqualTo(TeamRole.MEMBER);
        assertThat(response.status()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("only the invited user can accept")
    void otherUserCannotAccept() {
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitationFor(rahul)));

        assertThatThrownBy(() -> invitationService.accept(INVITATION_ID, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not sent to you");

        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("an invitation can only be answered once")
    void cannotAnswerTwice() {
        TeamInvitation invitation = pendingInvitationFor(rahul);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.decline(INVITATION_ID, 3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been accepted");
    }

    @Test
    @DisplayName("declining marks the invitation DECLINED without adding a member")
    void declineDoesNotAddMember() {
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitationFor(rahul)));

        InvitationResponse response = invitationService.decline(INVITATION_ID, 3L);

        assertThat(response.status()).isEqualTo(InvitationStatus.DECLINED);
        verify(teamMemberRepository, never()).save(any());
    }

    private TeamInvitation pendingInvitationFor(User invitedUser) {
        return TeamInvitation.builder()
                .id(INVITATION_ID)
                .team(team)
                .invitedUser(invitedUser)
                .invitedBy(priya)
                .status(InvitationStatus.PENDING)
                .build();
    }
}