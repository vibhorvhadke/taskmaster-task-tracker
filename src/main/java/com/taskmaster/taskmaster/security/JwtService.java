package com.taskmaster.taskmaster.security;

import com.taskmaster.taskmaster.config.JwtProperties;
import com.taskmaster.taskmaster.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Creates and validates JWTs (JSON Web Tokens).
 *
 * Token contents (claims):
 *   sub    -> user's email (the "subject" of the token)
 *   userId -> user's database ID
 *   jti    -> unique token ID, used to revoke the token on logout
 *   iat    -> issued-at time
 *   exp    -> expiration time
 */
@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        // Decode the Base64 secret and build an HMAC-SHA key.
        // Throws WeakKeyException at startup if the key is shorter than 256 bits,
        // so a bad configuration is caught immediately rather than at first login.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    /**
     * Creates a signed token for the given user.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim(USER_ID_CLAIM, user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies the token's signature and expiration, and returns its claims.
     *
     * @throws JwtException if the token is invalid, tampered with, or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Token lifetime in seconds, returned to the client in the login response. */
    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }
}