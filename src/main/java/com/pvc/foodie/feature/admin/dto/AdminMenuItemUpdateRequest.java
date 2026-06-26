package com.pvc.foodie.feature.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record AdminMenuItemUpdateRequest(
        UUID categoryId,
        @Size(max = 255) String name,
        String description,
        @DecimalMin("0.01") BigDecimal price,
        @Size(max = 255) String imageUrl,
        Boolean veg,
        Boolean available,
        Integer displayOrder) {
}
