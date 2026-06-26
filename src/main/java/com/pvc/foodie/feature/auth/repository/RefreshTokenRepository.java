package com.pvc.foodie.feature.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.auth.entity.RefreshToken;
import com.pvc.foodie.feature.auth.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    long deleteByExpiryDateBeforeOrRevokedTrue(Instant now);

    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
