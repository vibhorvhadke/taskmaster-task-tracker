package com.taskmaster.taskmaster.security;

import com.taskmaster.taskmaster.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Removes rows from revoked_tokens whose tokens have already expired.
 * An expired token is rejected by JwtService anyway, so its revocation record is no longer needed.
 *
 * Runs every hour (at minute 0), and requires @EnableScheduling on the application class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevokedTokenCleanupJob {

    private final RevokedTokenRepository revokedTokenRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = revokedTokenRepository.deleteAllExpired(Instant.now());
        if (deleted > 0) {
            log.info("Removed {} expired revoked token(s)", deleted);
        }
    }
}