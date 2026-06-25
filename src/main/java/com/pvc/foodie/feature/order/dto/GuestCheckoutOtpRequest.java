package com.pvc.foodie.feature.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestCheckoutOtpRequest(
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be a valid phone number") String phone) {
}
