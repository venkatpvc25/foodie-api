package com.pvc.foodie.feature.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.pvc.foodie.feature.coupon.entity.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CouponRequest(
        @NotBlank @Size(max = 80) String code,
        @Size(max = 1000) String description,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal discountValue,
        @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal maxDiscountAmount,
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal minOrderAmount,
        UUID restaurantId,
        boolean active,
        Instant validFrom,
        Instant validTo,
        @Min(1) Integer usageLimit) {
}
