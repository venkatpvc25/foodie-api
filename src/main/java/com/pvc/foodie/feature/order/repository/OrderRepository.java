package com.pvc.foodie.feature.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentStatus;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByIdDesc(UUID userId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    List<Order> findByRestaurantCreatedByIdOrderByIdDesc(UUID userId);

    Optional<Order> findByIdAndRestaurantCreatedById(UUID id, UUID userId);

    List<Order> findByStatusAndDeliveryPartnerIsNullOrderByIdDesc(OrderStatus status);

    List<Order> findByStatusAndPaymentStatusAndDeliveryPartnerIsNullOrderByIdDesc(
            OrderStatus status,
            PaymentStatus paymentStatus);

    List<Order> findByDeliveryPartnerIdOrderByIdDesc(UUID deliveryPartnerId);

    Optional<Order> findByIdAndDeliveryPartnerId(UUID id, UUID deliveryPartnerId);
}
