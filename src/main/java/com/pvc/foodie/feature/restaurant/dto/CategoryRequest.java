package com.pvc.foodie.feature.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 255) String name,
        int displayOrder) {
}
