package com.pvc.foodie.feature.order.dto;

public record RazorpayTransferPlan(
        Long restaurantAmount,
        Long platformCommissionAmount,
        Long adminTransferAmount,
        String restaurantAccountId,
        String adminAccountId) {
}
