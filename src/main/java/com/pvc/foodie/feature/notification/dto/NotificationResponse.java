package com.pvc.foodie.feature.notification.dto;

import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        boolean read,
        String type,
        String targetType,
        UUID targetId,
        String route) {
}
