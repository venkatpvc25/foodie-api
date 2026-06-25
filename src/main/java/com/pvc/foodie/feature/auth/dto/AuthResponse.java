package com.pvc.foodie.feature.auth.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String role;
}
