package com.pvc.foodie.feature.restaurant.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RestaurantResponse(
                UUID id,
                String name,
                String description,
                String imageUrl,
                String phone,
                String address,
                BigDecimal latitude,
                BigDecimal longitude,
                boolean open,
                double averageRating,
                long ratingCount) {
}
