package com.pvc.foodie.feature.admin.dto;

import java.util.UUID;

import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentStatus;

public record AdminOrderUpdateRequest(
        OrderStatus status,
        PaymentStatus paymentStatus,
        UUID deliveryPartnerId) {
}
