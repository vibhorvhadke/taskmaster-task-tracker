package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.TeamMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for team memberships.
 */
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /** Checks whether a user belongs to a team (used for access control). */
    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    /** Finds a user's membership in a team, e.g. to check if they are the OWNER. */
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    /** All members of a team, loading each member's User in the same query. */
    @EntityGraph(attributePaths = "user")
    List<TeamMember> findByTeamId(Long teamId);

    /** All teams a user belongs to, with each team and its creator loaded in the same query. */
    @EntityGraph(attributePaths = {"team", "team.createdBy"})
    List<TeamMember> findByUserIdOrderByTeamNameAsc(Long userId);
}
