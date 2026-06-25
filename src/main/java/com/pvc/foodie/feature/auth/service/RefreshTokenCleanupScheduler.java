package com.pvc.foodie.feature.auth.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.feature.auth.repository.RefreshTokenRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Scheduled(cron = "${app.refresh-token-cleanup.cron:0 0 3 * * *}")
    public void deleteExpiredOrRevokedRefreshTokens() {
        long deletedCount = refreshTokenRepository.deleteByExpiryDateBeforeOrRevokedTrue(Instant.now());
        log.info("Refresh token cleanup completed: deletedCount={}", deletedCount);
    }
}
