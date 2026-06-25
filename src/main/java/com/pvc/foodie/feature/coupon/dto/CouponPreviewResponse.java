package com.pvc.foodie.feature.coupon.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CouponPreviewResponse(
        UUID couponId,
        String code,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal discountedSubtotal) {
}
