package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.dto.task.TaskFilter;
import com.taskmaster.taskmaster.entity.Task;
import com.taskmaster.taskmaster.entity.Team;
import com.taskmaster.taskmaster.entity.TeamMember;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the dynamic WHERE clause for listing tasks.
 * Only the filters that are present in TaskFilter are added.
 */
public final class TaskSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private TaskSpecifications() {
    }

    public static Specification<Task> forUser(Long userId, TaskFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Always: only tasks this user is allowed to see
            predicates.add(visibleTo(userId, root, query, cb));

            // 2. Scope
            switch (filter.scope()) {
                case ASSIGNED_TO_ME -> predicates.add(cb.equal(root.get("assignee").get("id"), userId));
                case CREATED_BY_ME -> predicates.add(cb.equal(root.get("createdBy").get("id"), userId));
                case ALL -> { /* no extra condition */ }
            }

            // 3. Optional filters
            if (filter.teamId() != null) {
                predicates.add(cb.equal(root.get("team").get("id"), filter.teamId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.priority()));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * A task is visible if:
     *  - it belongs to a team the user is a member of, OR
     *  - it is a personal task (no team) created by the user.
     */
    private static Predicate visibleTo(Long userId, Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        // Sub-query: IDs of all teams the user belongs to
        Subquery<Long> myTeamIds = query.subquery(Long.class);
        Root<TeamMember> membership = myTeamIds.from(TeamMember.class);
        myTeamIds.select(membership.get("team").get("id"))
                .where(cb.equal(membership.get("user").get("id"), userId));

        // LEFT JOIN so that personal tasks (team = null) are not dropped
        Join<Task, Team> team = root.join("team", JoinType.LEFT);

        Predicate inMyTeam = team.get("id").in(myTeamIds);
        Predicate myPersonalTask = cb.and(
                cb.isNull(team.get("id")),
                cb.equal(root.get("createdBy").get("id"), userId)
        );

        return cb.or(inMyTeam, myPersonalTask);
    }

    /** Escapes LIKE wildcards so a search for "50%" matches the literal text "50%". */
    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}