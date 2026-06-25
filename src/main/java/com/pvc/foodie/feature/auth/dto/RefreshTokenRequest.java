package com.pvc.foodie.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
