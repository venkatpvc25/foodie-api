package com.pvc.foodie.feature.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
                UUID id,
                UUID menuItemId,
                String name,
                BigDecimal price,
                int quantity,
                BigDecimal lineTotal) {
}
