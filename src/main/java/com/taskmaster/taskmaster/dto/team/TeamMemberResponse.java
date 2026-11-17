package com.taskmaster.taskmaster.dto.team;

import com.taskmaster.taskmaster.entity.TeamMember;
import com.taskmaster.taskmaster.enums.TeamRole;

import java.time.Instant;

/**
 * One member of a team.
 */
public record TeamMemberResponse(
        Long userId,
        String fullName,
        String email,
        TeamRole role,
        Instant joinedAt
) {

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}