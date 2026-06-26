package com.pvc.foodie.feature.admin.dto;

import java.math.BigDecimal;
import java.util.List;

import com.pvc.foodie.feature.order.dto.OrderResponse;

public record AdminOrderDetailResponse(
        OrderResponse order,
        List<AdminPaymentTimelineEntry> paymentTimeline,
        String razorpayOrderId,
        String razorpayPaymentId,
        boolean razorpaySignatureCaptured,
        String restaurantRazorpayTransferId,
        String adminRazorpayTransferId,
        String deliveryPartnerRazorpayTransferId,
        String deliveryPayoutStatus,
        BigDecimal deliveryPartnerPayoutAmount,
        boolean refundSupported,
        boolean refundEligible,
        String refundStatus) {
}
