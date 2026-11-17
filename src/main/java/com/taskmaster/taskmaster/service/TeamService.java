package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.team.CreateTeamRequest;
import com.taskmaster.taskmaster.dto.team.TeamMemberResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Team creation, viewing, and team access checks.
 *
 * requireMembership / requireOwner are public because other services
 * (invitations, tasks, comments, attachments) reuse the same access rules.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    /**
     * Creates a team and adds the creator as its OWNER, in one transaction.
     */
    @Transactional
    public TeamResponse createTeam(Long userId, CreateTeamRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Team team = teamRepository.save(Team.builder()
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .createdBy(creator)
                .build());

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(creator)
                .role(TeamRole.OWNER)
                .build());

        return TeamResponse.from(team, TeamRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(Long userId) {
        return teamMemberRepository.findByUserIdOrderByTeamNameAsc(userId).stream()
                .map(membership -> TeamResponse.from(membership.getTeam(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId, Long userId) {
        TeamMember membership = requireMembership(teamId, userId);
        return TeamResponse.from(membership.getTeam(), membership.getRole());
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getMembers(Long teamId, Long userId) {
        requireMembership(teamId, userId);
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(TeamMemberResponse::from)
                .toList();
    }

    // ---------------------------------------------------------------------
    // Access checks (reused by other services)
    // ---------------------------------------------------------------------

    /**
     * Returns the user's membership in the team.
     *
     * @throws ResourceNotFoundException if the team does not exist (404)
     * @throws ForbiddenException        if the user is not a member (403)
     */
    @Transactional(readOnly = true)
    public TeamMember requireMembership(Long teamId, Long userId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team", teamId);
        }
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this team"));
    }

    /**
     * Same as requireMembership, but the user must also be the team OWNER.
     *
     * @throws ForbiddenException if the user is a member but not the OWNER (403)
     */
    @Transactional(readOnly = true)
    public TeamMember requireOwner(Long teamId, Long userId) {
        TeamMember membership = requireMembership(teamId, userId);
        if (membership.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("Only the team owner can perform this action");
        }
        return membership;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}