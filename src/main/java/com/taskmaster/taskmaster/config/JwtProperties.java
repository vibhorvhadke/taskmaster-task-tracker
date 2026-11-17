package com.taskmaster.taskmaster.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings loaded from application.properties (prefix "app.jwt").
 *
 * app.jwt.secret             -> secret          (Base64-encoded signing key, at least 256 bits)
 * app.jwt.expiration-minutes -> expirationMinutes
 *
 * @Validated makes the application fail at startup if a value is missing or invalid,
 * instead of failing later when the first token is created.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank(message = "app.jwt.secret must be set")
        String secret,

        @Positive(message = "app.jwt.expiration-minutes must be greater than 0")
        long expirationMinutes
) {
}