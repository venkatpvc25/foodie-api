package com.pvc.foodie.feature.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentMethod;
import com.pvc.foodie.feature.order.entity.PaymentStatus;

public record OrderResponse(
                UUID id,
                String orderNumber,
                UUID customerId,
                String customerName,
                UUID restaurantId,
                String restaurantName,
                UUID deliveryPartnerId,
                String deliveryPartnerName,
                UUID addressId,
                OrderStatus status,
                BigDecimal subtotal,
                BigDecimal deliveryCharge,
                BigDecimal tax,
                BigDecimal discountAmount,
                String couponCode,
                BigDecimal total,
                PaymentMethod paymentMethod,
                PaymentStatus paymentStatus,
                RazorpayCheckoutResponse razorpayCheckout,
                BigDecimal restaurantPayoutAmount,
                BigDecimal platformCommissionAmount,
                BigDecimal deliveryPartnerEarningAmount,
                BigDecimal deliveryPartnerTipAmount,
                BigDecimal deliveryPartnerPayoutAmount,
                String deliveryPartnerRazorpayTransferId,
                String notes,
                List<OrderItemResponse> items) {
}
