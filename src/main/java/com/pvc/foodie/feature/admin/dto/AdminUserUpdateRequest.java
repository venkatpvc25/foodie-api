package com.pvc.foodie.feature.admin.dto;

import com.pvc.foodie.feature.auth.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @Size(max = 255) String name,
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be a valid phone number") String phone,
        @Email @Size(max = 255) String email,
        Role role,
        Boolean blocked,
        @Size(max = 255) String razorpayLinkedAccountId) {
}
