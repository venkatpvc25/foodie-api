package com.pvc.foodie.feature.coupon.dto;

import java.math.BigDecimal;

import com.pvc.foodie.feature.coupon.entity.Coupon;

public record AppliedCoupon(
        Coupon coupon,
        String code,
        BigDecimal discountAmount) {

    public static AppliedCoupon none() {
        return new AppliedCoupon(null, null, BigDecimal.ZERO);
    }
}
