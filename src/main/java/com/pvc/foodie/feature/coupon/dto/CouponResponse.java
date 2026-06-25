package com.pvc.foodie.feature.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.pvc.foodie.feature.coupon.entity.DiscountType;

public record CouponResponse(
        UUID id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        UUID restaurantId,
        String restaurantName,
        boolean active,
        Instant validFrom,
        Instant validTo,
        Integer usageLimit,
        int usedCount) {
}
