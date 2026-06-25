package com.pvc.foodie.feature.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByIdDesc(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndTitleAndMessage(UUID userId, String title, String message);
}
