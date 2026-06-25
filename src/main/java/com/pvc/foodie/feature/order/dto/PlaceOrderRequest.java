package com.pvc.foodie.feature.order.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
                @NotNull UUID addressId,
                @Size(max = 80) String couponCode,
                @Size(max = 1000) String notes) {
}
