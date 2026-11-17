package com.taskmaster.taskmaster.security;

import com.taskmaster.taskmaster.config.JwtProperties;
import com.taskmaster.taskmaster.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests for JwtService (no Spring context, no database needed).
 */
class JwtServiceTest {

    // 64 bytes of test-only key material, Base64-encoded (512 bits, well above the 256-bit minimum)
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-for-unit-tests-only-0123456789-abcdefghijklmnop".getBytes());

    private final JwtService jwtService = new JwtService(new JwtProperties(TEST_SECRET, 60));

    private final User user = User.builder()
            .id(42L)
            .email("priya@example.com")
            .fullName("Priya Sharma")
            .build();

    @Test
    void generatedTokenCanBeParsedBackWithCorrectClaims() {
        String token = jwtService.generateToken(user);

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("priya@example.com");
        assertThat(claims.get("userId", Long.class)).isEqualTo(42L);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void eachTokenGetsAUniqueId() {
        String first = jwtService.generateToken(user);
        String second = jwtService.generateToken(user);

        assertThat(jwtService.parseToken(first).getId())
                .isNotEqualTo(jwtService.parseToken(second).getId());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user);
        // Change one character in the signature part (after the last dot)
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        String otherSecret = Base64.getEncoder()
                .encodeToString("another-secret-key-for-tests-only-9876543210-zyxwvutsrqponmlkj".getBytes());
        JwtService otherService = new JwtService(new JwtProperties(otherSecret, 60));
        String foreignToken = otherService.generateToken(user);

        assertThatThrownBy(() -> jwtService.parseToken(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        // Negative lifetime -> the token is already expired when created
        JwtService expiringService = new JwtService(new JwtProperties(TEST_SECRET, -1));
        String expiredToken = expiringService.generateToken(user);

        assertThatThrownBy(() -> jwtService.parseToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSecretIsRejectedAtConstruction() {
        String weakSecret = Base64.getEncoder().encodeToString("123".getBytes());

        assertThatThrownBy(() -> new JwtService(new JwtProperties(weakSecret, 60)))
                .isInstanceOf(WeakKeyException.class);
    }
}