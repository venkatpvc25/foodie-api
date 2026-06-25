package com.pvc.foodie.feature.cart.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(@NotNull UUID menuItemId, @Min(1) int quantity) {
}
