package com.pvc.foodie.feature.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.auth.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    long deleteByExpiryDateBeforeOrRevokedTrue(Instant now);
}
