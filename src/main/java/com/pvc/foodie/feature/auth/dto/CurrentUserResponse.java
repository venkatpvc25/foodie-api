package com.pvc.foodie.feature.auth.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserResponse {
    private UUID userId;
    private String name;
    private String phone;
    private String email;
    private String role;
}
