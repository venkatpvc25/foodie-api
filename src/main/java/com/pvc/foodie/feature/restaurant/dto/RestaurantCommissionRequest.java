package com.pvc.foodie.feature.restaurant.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RestaurantCommissionRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal commissionRate) {
}
