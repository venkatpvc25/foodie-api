package com.pvc.foodie.feature.order.dto;

public record GuestCheckoutOtpResponse(
        String phone,
        int expiresInSeconds,
        String debugVerificationCode) {
}
