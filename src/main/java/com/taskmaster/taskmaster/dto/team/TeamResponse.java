package com.taskmaster.taskmaster.dto.team;

import com.taskmaster.taskmaster.dto.user.UserSummary;
import com.taskmaster.taskmaster.entity.Team;
import com.taskmaster.taskmaster.enums.TeamRole;

import java.time.Instant;

/**
 * Team details as seen by the logged-in user, including their role in the team.
 */
public record TeamResponse(
        Long id,
        String name,
        String description,
        UserSummary createdBy,
        TeamRole myRole,
        Instant createdAt
) {

    public static TeamResponse from(Team team, TeamRole myRole) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                UserSummary.from(team.getCreatedBy()),
                myRole,
                team.getCreatedAt()
        );
    }
}