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
        if (order.getPaymentMethod() == PaymentMethod.ONLINE && order.getRazorpayOrderId() == null) {
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
        if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay order can be created only for online payments");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order payment is already completed");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cancelled order cannot be paid");
        }
    }

    private void verifyRazorpayPaymentForOrder(Order order, VerifyRazorpayPaymentRequest request) {
        if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay verification is only for online payments");
        }
        if (order.getRazorpayOrderId() == null || !order.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay order id does not match");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }
        boolean verified = razorpayGatewayService.verifySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature());
        if (!verified) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay payment verification failed");
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setRazorpayPaymentId(request.razorpayPaymentId());
        order.setRazorpaySignature(request.razorpaySignature());
    }

    private void requireCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.ADMIN) {
            log.warn("Razorpay payment access denied: userId={}, role={}", user.getId(), user.getRole());
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Customer access required");
        }
    }
}
