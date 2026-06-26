package com.pvc.foodie.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhoneLoginRequest(
                @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be a valid phone number") String phone,
                @NotBlank @Size(min = 6, max = 6) String verificationCode) {
}
