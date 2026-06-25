package com.pvc.foodie.feature.order.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pvc.foodie.feature.order.dto.OrderItemResponse;
import com.pvc.foodie.feature.order.dto.OrderResponse;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderResponseMapper {

    private final RazorpayGatewayService razorpayGatewayService;

    public OrderResponse toResponse(Order order) {
        UUID deliveryPartnerId = order.getDeliveryPartner() == null ? null : order.getDeliveryPartner().getId();
        String deliveryPartnerName = order.getDeliveryPartner() == null ? null : order.getDeliveryPartner().getName();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                deliveryPartnerId,
                deliveryPartnerName,
                order.getAddress().getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDeliveryCharge(),
                order.getTax(),
                order.getTotal(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                razorpayGatewayService.checkoutResponse(order),
                order.getRestaurantPayoutAmount(),
                order.getPlatformCommissionAmount(),
                deliveryPartnerEarningAmount(order),
                order.getDeliveryPartnerPayoutAmount(),
                order.getDeliveryPartnerRazorpayTransferId(),
                order.getNotes(),
                order.getItems().stream().map(this::toItemResponse).toList());
    }

    private BigDecimal deliveryPartnerEarningAmount(Order order) {
        return order.getDeliveryPartner() == null ? BigDecimal.ZERO : order.getDeliveryCharge();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
