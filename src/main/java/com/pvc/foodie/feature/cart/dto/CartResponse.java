package com.pvc.foodie.feature.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
                UUID id,
                UUID restaurantId,
                String restaurantName,
                List<CartItemResponse> items,
                BigDecimal subtotal) {
}
