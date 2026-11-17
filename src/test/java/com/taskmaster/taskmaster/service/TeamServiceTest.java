package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.team.CreateTeamRequest;
import com.taskmaster.taskmaster.dto.team.TeamResponse;
import com.taskmaster.taskmaster.entity.Team;
import com.taskmaster.taskmaster.entity.TeamMember;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.enums.TeamRole;
import com.taskmaster.taskmaster.exception.ForbiddenException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.repository.TeamMemberRepository;
import com.taskmaster.taskmaster.repository.TeamRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for team creation and the team access checks
 * (requireMembership / requireOwner) that every other service relies on.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    private static final Long TEAM_ID = 10L;

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TeamService teamService;

    private final User priya = User.builder().id(1L).fullName("Priya Sharma").email("priya@example.com").build();
    private final User amit = User.builder().id(2L).fullName("Amit Verma").email("amit@example.com").build();
    private final Team team = Team.builder().id(TEAM_ID).name("Website Launch").createdBy(priya).build();

    @Test
    @DisplayName("creating a team makes the creator its OWNER")
    void creatorBecomesOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(priya));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamResponse response = teamService.createTeam(1L, new CreateTeamRequest("  Website Launch ", "  "));

        ArgumentCaptor<TeamMember> saved = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(priya);
        assertThat(saved.getValue().getRole()).isEqualTo(TeamRole.OWNER);

        assertThat(response.name()).isEqualTo("Website Launch");
        assertThat(response.description()).isNull();          // blank description stored as null
        assertThat(response.myRole()).isEqualTo(TeamRole.OWNER);
    }

    @Test
    @DisplayName("requireMembership: missing team gives 404")
    void missingTeam() {
        when(teamRepository.existsById(TEAM_ID)).thenReturn(false);

        assertThatThrownBy(() -> teamService.requireMembership(TEAM_ID, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("requireMembership: non-member gives 403")
    void nonMember() {
        when(teamRepository.existsById(TEAM_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.requireMembership(TEAM_ID, 3L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You are not a member of this team");
    }

    @Test
    @DisplayName("requireOwner: a MEMBER gives 403")
    void memberIsNotOwner() {
        when(teamRepository.existsById(TEAM_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 2L))
                .thenReturn(Optional.of(TeamMember.builder().team(team).user(amit).role(TeamRole.MEMBER).build()));

        assertThatThrownBy(() -> teamService.requireOwner(TEAM_ID, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only the team owner");
    }

    @Test
    @DisplayName("requireOwner: the OWNER passes")
    void ownerPasses() {
        TeamMember ownerMembership = TeamMember.builder().team(team).user(priya).role(TeamRole.OWNER).build();
        when(teamRepository.existsById(TEAM_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 1L)).thenReturn(Optional.of(ownerMembership));

        assertThat(teamService.requireOwner(TEAM_ID, 1L)).isSameAs(ownerMembership);
    }
}