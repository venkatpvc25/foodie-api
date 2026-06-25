package com.pvc.foodie.feature.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.notification.entity.NotificationDeviceToken;

public interface NotificationDeviceTokenRepository extends JpaRepository<NotificationDeviceToken, UUID> {
    List<NotificationDeviceToken> findByUserId(UUID userId);

    Optional<NotificationDeviceToken> findByToken(String token);

    Optional<NotificationDeviceToken> findByUserIdAndToken(UUID userId, String token);
}
