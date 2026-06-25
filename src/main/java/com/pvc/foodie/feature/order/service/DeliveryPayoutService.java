package com.pvc.foodie.feature.order.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.notification.event.DeliveryPayoutTransferredEvent;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryPayoutService {

    private static final String RAZORPAY_ACCOUNT_ID_PATTERN = "^acc_[A-Za-z0-9]{14}$";

    private final RazorpayGatewayService razorpayGatewayService;
    private final ApplicationEventPublisher eventPublisher;

    public void transferDeliveryChargeIfDelivered(Order order) {
        if (!isDelivered(order)) {
            return;
        }
        if (hasDeliveryPayoutTransfer(order)) {
            return;
        }

        requirePaidRazorpayPayment(order);
        User deliveryPartner = requireDeliveryPartner(order);

        String accountId = normalizeAccountId(deliveryPartner.getRazorpayLinkedAccountId());
        requireConfiguredAccountId(accountId);

        String transferId = razorpayGatewayService.transferDeliveryCharge(order, accountId);
        order.setDeliveryPartnerPayoutAmount(order.getDeliveryCharge());
        order.setDeliveryPartnerRazorpayTransferId(transferId);
        log.info("Delivery payout transferred: orderId={}, deliveryPartnerId={}, transferId={}, amount={}",
                order.getId(), deliveryPartner.getId(), transferId, order.getDeliveryCharge());
        eventPublisher.publishEvent(new DeliveryPayoutTransferredEvent(order));
    }

    private boolean isDelivered(Order order) {
        return order.getStatus() == OrderStatus.DELIVERED;
    }

    private boolean hasDeliveryPayoutTransfer(Order order) {
        return order.getDeliveryPartnerRazorpayTransferId() != null;
    }

    private void requirePaidRazorpayPayment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID || order.getRazorpayPaymentId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Paid Razorpay payment is required for delivery payout");
        }
    }

    private User requireDeliveryPartner(Order order) {
        User deliveryPartner = order.getDeliveryPartner();
        if (deliveryPartner == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Delivery partner is required for delivery payout");
        }
        return deliveryPartner;
    }

    private String normalizeAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        return accountId.trim();
    }

    private void requireConfiguredAccountId(String accountId) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Delivery partner Razorpay linked account is not configured");
        }
        if (!accountId.matches(RAZORPAY_ACCOUNT_ID_PATTERN)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Delivery partner Razorpay linked account id is invalid. Expected format acc_XXXXXXXXXXXXXX");
        }
    }
}
