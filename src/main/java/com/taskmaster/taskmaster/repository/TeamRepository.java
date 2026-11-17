package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data access for teams.
 */
public interface TeamRepository extends JpaRepository<Team, Long> {

    /** All teams the given user is a member of (as OWNER or MEMBER). */
    @Query("SELECT tm.team FROM TeamMember tm WHERE tm.user.id = :userId ORDER BY tm.team.name")
    List<Team> findAllByMemberId(@Param("userId") Long userId);
}
