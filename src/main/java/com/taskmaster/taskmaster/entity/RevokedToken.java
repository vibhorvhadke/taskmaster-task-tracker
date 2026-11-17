package com.taskmaster.taskmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A JWT that was invalidated on logout.
 * Any request using a token whose ID is stored here is rejected.
 * Rows can be deleted once expiresAt has passed.
 * Mapped to the "revoked_tokens" table.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The unique ID (jti claim) of the revoked JWT. */
    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    private String tokenId;

    /** When the token would have expired naturally. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}