package com.pvc.foodie.feature.rating.dto;

import java.time.Instant;
import java.util.UUID;

public record MenuItemRatingResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        String customerName,
        UUID restaurantId,
        UUID menuItemId,
        String menuItemName,
        int rating,
        String comment,
        Instant createdAt) {
}
