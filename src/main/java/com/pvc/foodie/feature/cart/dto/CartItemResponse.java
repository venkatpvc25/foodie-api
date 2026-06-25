package com.pvc.foodie.feature.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
                UUID id,
                UUID menuItemId,
                String name,
                BigDecimal price,
                int quantity,
                BigDecimal lineTotal) {
}
