package com.taskmaster.taskmaster.dto.auth;

import com.taskmaster.taskmaster.dto.user.UserResponse;

/**
 * Response body after successful registration or login.
 *
 * Example:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 3600,
 *   "user": { "id": 1, "fullName": "Priya Sharma", "email": "priya@example.com", ... }
 * }
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,      // seconds until the token expires
        UserResponse user
) {

    public static AuthResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}