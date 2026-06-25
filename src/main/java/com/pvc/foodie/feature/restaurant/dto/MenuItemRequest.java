package com.pvc.foodie.feature.restaurant.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MenuItemRequest(
        UUID categoryId,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Size(max = 255) String imageUrl,
        boolean veg,
        boolean available,
        int displayOrder) {
}
