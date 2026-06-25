package com.pvc.foodie.feature.notification.event;

import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderStatus;

public record OrderStatusChangedEvent(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
}
