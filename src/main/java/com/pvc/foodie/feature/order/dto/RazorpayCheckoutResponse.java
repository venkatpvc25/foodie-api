package com.pvc.foodie.feature.order.dto;

public record RazorpayCheckoutResponse(
        String keyId,
        String razorpayOrderId,
        Long amount,
        String currency,
        String receipt) {
}
