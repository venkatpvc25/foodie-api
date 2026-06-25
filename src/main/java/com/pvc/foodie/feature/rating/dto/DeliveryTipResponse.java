package com.pvc.foodie.feature.rating.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DeliveryTipResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        String customerName,
        UUID deliveryPartnerId,
        String deliveryPartnerName,
        BigDecimal amount,
        String note,
        Instant createdAt) {
}
