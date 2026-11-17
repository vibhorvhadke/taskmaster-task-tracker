package com.taskmaster.taskmaster.dto.user;

import com.taskmaster.taskmaster.entity.User;

import java.time.Instant;

/**
 * Safe, public view of a user. Never includes the password.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Instant createdAt,
        Instant updatedAt
) {

    /** Converts a User entity into a UserResponse. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}