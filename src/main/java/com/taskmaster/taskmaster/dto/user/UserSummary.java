package com.taskmaster.taskmaster.dto.user;

import com.taskmaster.taskmaster.entity.User;

/**
 * Short user info embedded in other responses (team creator, task assignee, comment author, ...).
 */
public record UserSummary(
        Long id,
        String fullName,
        String email
) {

    /** Returns null for a null user, e.g. an unassigned task. */
    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail());
    }
}