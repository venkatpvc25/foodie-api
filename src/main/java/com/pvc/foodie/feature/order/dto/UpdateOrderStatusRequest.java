package com.pvc.foodie.feature.order.dto;

import com.pvc.foodie.feature.order.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
