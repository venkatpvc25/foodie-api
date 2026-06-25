package com.pvc.foodie.feature.restaurant.dto;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, int displayOrder) {
}
