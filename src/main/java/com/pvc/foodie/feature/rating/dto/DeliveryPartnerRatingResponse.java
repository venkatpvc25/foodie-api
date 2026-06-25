package com.pvc.foodie.feature.rating.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryPartnerRatingResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        String customerName,
        UUID deliveryPartnerId,
        String deliveryPartnerName,
        int rating,
        String comment,
        Instant createdAt) {
}
