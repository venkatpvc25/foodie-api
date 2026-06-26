package com.pvc.foodie.feature.restaurant.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkMenuUploadRequest(
        @NotEmpty @Size(max = 500) List<@Valid BulkMenuItemRequest> items) {
}
