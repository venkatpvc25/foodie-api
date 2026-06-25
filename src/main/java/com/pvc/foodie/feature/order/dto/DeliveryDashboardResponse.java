package com.pvc.foodie.feature.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record DeliveryDashboardResponse(
        long activeOrderCount,
        long deliveredOrderCount,
        BigDecimal totalEarnings,
        List<OrderResponse> activeOrders,
        List<OrderResponse> deliveredOrders) {
}
