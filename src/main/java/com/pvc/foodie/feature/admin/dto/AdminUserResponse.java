package com.pvc.foodie.feature.admin.dto;

import java.util.UUID;

import com.pvc.foodie.feature.auth.entity.Role;

public record AdminUserResponse(
        UUID id,
        String name,
        String phone,
        String email,
        Role role,
        boolean blocked,
        boolean razorpayReady,
        String razorpayLinkedAccountId) {
}
