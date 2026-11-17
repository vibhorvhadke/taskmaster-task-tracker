package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Data access for revoked (logged-out) JWTs.
 */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    /** Checked on every authenticated request: is this token logged out? */
    boolean existsByTokenId(String tokenId);

    /** Removes rows for tokens that have already expired (they can no longer be used anyway). */
    @Modifying
    @Query("DELETE FROM RevokedToken rt WHERE rt.expiresAt < :now")
    int deleteAllExpired(@Param("now") Instant now);
}
