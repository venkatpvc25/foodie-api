package com.pvc.foodie.feature.rating.dto;

import java.time.Instant;
import java.util.UUID;

public record RestaurantRatingResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        String customerName,
        UUID restaurantId,
        int rating,
        String comment,
        Instant createdAt) {
}
