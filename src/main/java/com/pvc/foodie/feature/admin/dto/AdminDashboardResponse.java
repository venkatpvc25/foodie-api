package com.pvc.foodie.feature.admin.dto;

import java.math.BigDecimal;

public record AdminDashboardResponse(
        long userCount,
        long customerCount,
        long restaurantUserCount,
        long deliveryPartnerCount,
        long restaurantCount,
        long orderCount,
        long paidOrderCount,
        BigDecimal grossOrderValue,
        BigDecimal platformCommissionTotal,
        BigDecimal restaurantPayoutTotal,
        BigDecimal deliveryPayoutTotal,
        long activeCouponCount) {
}
