package com.pvc.foodie.feature.order.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.notification.event.PaymentVerifiedEvent;
import com.pvc.foodie.feature.order.dto.OrderResponse;
import com.pvc.foodie.feature.order.dto.VerifyRazorpayPaymentRequest;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentMethod;
import com.pvc.foodie.feature.order.entity.PaymentStatus;
import com.pvc.foodie.feature.order.repository.OrderRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayPaymentService {

    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    private final RazorpayGatewayService razorpayGatewayService;
    private final OrderResponseMapper orderResponseMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void createOnlinePaymentOrderIfRequired(Order order) {
        if (requiresRazorpayOrder(order)) {
            razorpayGatewayService.createOrder(order);
        }
    }

    @Transactional
    public OrderResponse createRazorpayOrder(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        validatePayableOnlineOrder(order);
        createOnlinePaymentOrderIfRequired(order);
        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse verifyRazorpayPayment(UUID id, VerifyRazorpayPaymentRequest request) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        verifyRazorpayPaymentForOrder(order, request);
        log.info("Razorpay payment verified: userId={}, orderId={}, razorpayOrderId={}, razorpayPaymentId={}",
                user.getId(), order.getId(), request.razorpayOrderId(), request.razorpayPaymentId());
        eventPublisher.publishEvent(new PaymentVerifiedEvent(order));
        return orderResponseMapper.toResponse(order);
    }

    private void validatePayableOnlineOrder(Order order) {
        if (!isOnlinePayment(order)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay order can be created only for online payments");
        }
        if (isPaymentCompleted(order)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order payment is already completed");
        }
        if (isCancelled(order)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cancelled order cannot be paid");
        }
    }

    private void verifyRazorpayPaymentForOrder(Order order, VerifyRazorpayPaymentRequest request) {
        requireOnlinePayment(order);
        requireMatchingRazorpayOrderId(order, request);
        if (isPaymentCompleted(order)) {
            return;
        }
        if (!isValidRazorpaySignature(request)) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay payment verification failed");
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setRazorpayPaymentId(request.razorpayPaymentId());
        order.setRazorpaySignature(request.razorpaySignature());
    }

    private void requireOnlinePayment(Order order) {
        if (!isOnlinePayment(order)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay verification is only for online payments");
        }
    }

    private void requireMatchingRazorpayOrderId(Order order, VerifyRazorpayPaymentRequest request) {
        if (!hasMatchingRazorpayOrderId(order, request)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay order id does not match");
        }
    }

    private boolean isValidRazorpaySignature(VerifyRazorpayPaymentRequest request) {
        return razorpayGatewayService.verifySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature());
    }

    private boolean isOnlinePayment(Order order) {
        return order.getPaymentMethod() == PaymentMethod.ONLINE;
    }

    private boolean requiresRazorpayOrder(Order order) {
        return isOnlinePayment(order) && order.getRazorpayOrderId() == null;
    }

    private boolean isPaymentCompleted(Order order) {
        return order.getPaymentStatus() == PaymentStatus.PAID;
    }

    private boolean isCancelled(Order order) {
        return order.getStatus() == OrderStatus.CANCELLED;
    }

    private boolean hasMatchingRazorpayOrderId(Order order, VerifyRazorpayPaymentRequest request) {
        return order.getRazorpayOrderId() != null && order.getRazorpayOrderId().equals(request.razorpayOrderId());
    }

    private void requireCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.ADMIN) {
            log.warn("Razorpay payment access denied: userId={}, role={}", user.getId(), user.getRole());
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Customer access required");
        }
    }
}
