package com.taskmaster.taskmaster.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/teams.
 */
public record CreateTeamRequest(

        @NotBlank(message = "Team name is required")
        @Size(max = 100, message = "Team name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
) {
}