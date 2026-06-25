package com.pvc.foodie.feature.notification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.notification.dto.NotificationResponse;
import com.pvc.foodie.feature.notification.entity.Notification;
import com.pvc.foodie.feature.notification.repository.NotificationRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public List<NotificationResponse> getNotifications() {
        User user = currentUserService.getCurrentUser();
        List<NotificationResponse> notifications = notificationRepository.findByUserIdOrderByIdDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
        log.info("Notifications fetched: userId={}, count={}", user.getId(), notifications.size());
        return notifications;
    }

    @Transactional
    public NotificationResponse markRead(UUID id) {
        User user = currentUserService.getCurrentUser();
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        log.info("Notification marked read: userId={}, notificationId={}", user.getId(), id);
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getType(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getRoute());
    }
}
