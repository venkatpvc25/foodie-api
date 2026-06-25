package com.pvc.foodie.feature.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(@Min(1) int quantity) {
}
