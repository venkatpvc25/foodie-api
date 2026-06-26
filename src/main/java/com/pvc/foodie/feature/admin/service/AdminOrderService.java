package com.pvc.foodie.feature.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.admin.dto.AdminOrderDetailResponse;
import com.pvc.foodie.feature.admin.dto.AdminPaymentTimelineEntry;
import com.pvc.foodie.feature.admin.dto.AdminOrderUpdateRequest;
import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;
import com.pvc.foodie.feature.audit.service.AuditLogService;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.auth.repository.UserRepository;
import com.pvc.foodie.feature.order.dto.OrderResponse;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentStatus;
import com.pvc.foodie.feature.order.repository.OrderRepository;
import com.pvc.foodie.feature.order.service.DeliveryPayoutService;
import com.pvc.foodie.feature.order.service.OrderResponseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final AdminAccessService adminAccessService;
    private final AuditLogService auditLogService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderResponseMapper orderResponseMapper;
    private final DeliveryPayoutService deliveryPayoutService;

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(
            OrderStatus status,
            PaymentStatus paymentStatus,
            UUID restaurantId,
            UUID customerId,
            UUID deliveryPartnerId) {
        adminAccessService.requireAdmin();
        return orderRepository.findAll().stream()
                .filter(order -> status == null || order.getStatus() == status)
                .filter(order -> paymentStatus == null || order.getPaymentStatus() == paymentStatus)
                .filter(order -> restaurantId == null || order.getRestaurant().getId().equals(restaurantId))
                .filter(order -> customerId == null || order.getUser().getId().equals(customerId))
                .filter(order -> deliveryPartnerId == null
                        || (order.getDeliveryPartner() != null
                                && order.getDeliveryPartner().getId().equals(deliveryPartnerId)))
                .map(orderResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrder(UUID id) {
        adminAccessService.requireAdmin();
        return toDetailResponse(requireOrder(id));
    }

    @Transactional
    public OrderResponse updateOrder(UUID id, AdminOrderUpdateRequest request) {
        User admin = adminAccessService.requireAdmin();
        Order order = requireOrder(id);
        OrderStatus oldStatus = order.getStatus();
        PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        UUID oldDeliveryPartnerId = order.getDeliveryPartner() == null ? null : order.getDeliveryPartner().getId();
        if (request.status() != null) {
            order.setStatus(request.status());
        }
        if (request.paymentStatus() != null) {
            order.setPaymentStatus(request.paymentStatus());
        }
        if (request.deliveryPartnerId() != null) {
            order.setDeliveryPartner(requireDeliveryPartner(request.deliveryPartnerId()));
        }
        deliveryPayoutService.transferDeliveryChargeIfDelivered(order);
        recordOrderAudit(admin, order, oldStatus, oldPaymentStatus, oldDeliveryPartnerId);
        log.info("Admin order updated: adminId={}, orderId={}, status={}, paymentStatus={}, deliveryPartnerId={}",
                admin.getId(), order.getId(), order.getStatus(), order.getPaymentStatus(),
                order.getDeliveryPartner() == null ? null : order.getDeliveryPartner().getId());
        return orderResponseMapper.toResponse(order);
    }

    private Order requireOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
    }

    private AdminOrderDetailResponse toDetailResponse(Order order) {
        return new AdminOrderDetailResponse(
                orderResponseMapper.toResponse(order),
                paymentTimeline(order),
                order.getRazorpayOrderId(),
                order.getRazorpayPaymentId(),
                hasText(order.getRazorpaySignature()),
                order.getRestaurantRazorpayTransferId(),
                order.getAdminRazorpayTransferId(),
                order.getDeliveryPartnerRazorpayTransferId(),
                deliveryPayoutStatus(order),
                order.getDeliveryPartnerPayoutAmount(),
                false,
                false,
                "NOT_SUPPORTED");
    }

    private List<AdminPaymentTimelineEntry> paymentTimeline(Order order) {
        return List.of(
                new AdminPaymentTimelineEntry("ORDER_CREATED", order.getRazorpayOrderId() != null,
                        order.getRazorpayOrderId()),
                new AdminPaymentTimelineEntry("PAYMENT_CAPTURED", order.getRazorpayPaymentId() != null,
                        order.getRazorpayPaymentId()),
                new AdminPaymentTimelineEntry("RESTAURANT_TRANSFERRED", order.getRestaurantRazorpayTransferId() != null,
                        order.getRestaurantRazorpayTransferId()),
                new AdminPaymentTimelineEntry("PLATFORM_TRANSFERRED", order.getAdminRazorpayTransferId() != null,
                        order.getAdminRazorpayTransferId()),
                new AdminPaymentTimelineEntry("DELIVERY_PAYOUT_TRANSFERRED",
                        order.getDeliveryPartnerRazorpayTransferId() != null,
                        order.getDeliveryPartnerRazorpayTransferId()));
    }

    private String deliveryPayoutStatus(Order order) {
        if (order.getDeliveryPartnerRazorpayTransferId() != null) {
            return "TRANSFERRED";
        }
        if (order.getDeliveryPartner() == null) {
            return "NO_DELIVERY_PARTNER";
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return "WAITING_FOR_DELIVERY";
        }
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return "WAITING_FOR_PAYMENT";
        }
        if (!hasText(order.getDeliveryPartner().getRazorpayLinkedAccountId())) {
            return "MISSING_DELIVERY_RAZORPAY_ACCOUNT";
        }
        return "PENDING_TRANSFER";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private User requireDeliveryPartner(UUID deliveryPartnerId) {
        User user = userRepository.findById(deliveryPartnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Delivery partner not found"));
        if (user.getRole() != Role.DELIVERY_PARTNER && user.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Delivery partner user is required");
        }
        return user;
    }

    private void recordOrderAudit(
            User admin,
            Order order,
            OrderStatus oldStatus,
            PaymentStatus oldPaymentStatus,
            UUID oldDeliveryPartnerId) {
        if (oldStatus != order.getStatus()) {
            auditLogService.record(
                    admin,
                    AuditAction.ORDER_STATUS_CHANGED,
                    AuditEntityType.ORDER,
                    order.getId(),
                    order.getOrderNumber(),
                    "Status changed from " + oldStatus + " to " + order.getStatus());
        }
        if (oldPaymentStatus != order.getPaymentStatus()) {
            auditLogService.record(
                    admin,
                    AuditAction.ORDER_PAYMENT_STATUS_CHANGED,
                    AuditEntityType.ORDER,
                    order.getId(),
                    order.getOrderNumber(),
                    "Payment status changed from " + oldPaymentStatus + " to " + order.getPaymentStatus());
        }
        UUID newDeliveryPartnerId = order.getDeliveryPartner() == null ? null : order.getDeliveryPartner().getId();
        if ((oldDeliveryPartnerId == null && newDeliveryPartnerId != null)
                || (oldDeliveryPartnerId != null && !oldDeliveryPartnerId.equals(newDeliveryPartnerId))) {
            auditLogService.record(
                    admin,
                    AuditAction.ORDER_DELIVERY_PARTNER_CHANGED,
                    AuditEntityType.ORDER,
                    order.getId(),
                    order.getOrderNumber(),
                    "Delivery partner changed from " + oldDeliveryPartnerId + " to " + newDeliveryPartnerId);
        }
    }
}
