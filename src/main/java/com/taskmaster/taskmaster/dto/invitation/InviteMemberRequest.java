package com.taskmaster.taskmaster.dto.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/teams/{teamId}/invitations.
 * The invited person must already have a TaskMaster account.
 */
public record InviteMemberRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) {
}