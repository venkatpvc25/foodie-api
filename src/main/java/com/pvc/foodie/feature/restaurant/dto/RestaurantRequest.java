package com.pvc.foodie.feature.restaurant.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestaurantRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 255) String imageUrl,
        @Size(max = 20) String phone,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean open,
        @Size(max = 255) String razorpayLinkedAccountId) {
}
