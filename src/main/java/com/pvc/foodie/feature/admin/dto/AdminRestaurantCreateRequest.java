package com.pvc.foodie.feature.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRestaurantCreateRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 255) String imageUrl,
        @Size(max = 20) String phone,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean open,
        boolean approved,
        boolean suspended,
        @NotNull UUID ownerId,
        @Size(max = 255) String razorpayLinkedAccountId,
        @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal commissionRate) {
}
