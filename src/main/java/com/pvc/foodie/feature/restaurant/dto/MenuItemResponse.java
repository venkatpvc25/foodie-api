package com.pvc.foodie.feature.restaurant.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
                UUID id,
                UUID categoryId,
                String name,
                String description,
                BigDecimal price,
                String imageUrl,
                boolean veg,
                boolean available,
                int displayOrder) {
}
