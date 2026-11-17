package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.TeamInvitation;
import com.taskmaster.taskmaster.enums.InvitationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for team invitations.
 */
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    /** Invitations received by a user with a given status, e.g. all PENDING ones. */
    @EntityGraph(attributePaths = {"team", "invitedUser", "invitedBy"})
    List<TeamInvitation> findByInvitedUserIdAndStatus(Long invitedUserId, InvitationStatus status);

    /** Prevents sending a duplicate PENDING invitation. */
    boolean existsByTeamIdAndInvitedUserIdAndStatus(Long teamId, Long invitedUserId, InvitationStatus status);

}
