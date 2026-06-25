package com.pvc.foodie.feature.notification.event;

import com.pvc.foodie.feature.order.entity.Order;

public record PaymentVerifiedEvent(Order order) {
}
