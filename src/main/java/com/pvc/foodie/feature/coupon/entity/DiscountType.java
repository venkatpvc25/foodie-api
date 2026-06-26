package com.pvc.foodie.feature.coupon.entity;

public enum DiscountType {
    PERCENTAGE,
    FIXED,
    FLAT;

    public boolean isPercentage() {
        return this == PERCENTAGE;
    }
}
