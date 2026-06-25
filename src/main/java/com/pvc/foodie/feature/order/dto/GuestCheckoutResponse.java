package com.pvc.foodie.feature.order.dto;

import com.pvc.foodie.feature.auth.dto.AuthResponse;

public record GuestCheckoutResponse(
                OrderResponse order,
                AuthResponse auth,
                boolean phoneVerified) {
}
