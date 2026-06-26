package com.pvc.foodie.feature.restaurant.dto;

import java.util.List;

public record BulkMenuUploadResponse(
        int createdCount,
        List<MenuItemResponse> items) {
}
