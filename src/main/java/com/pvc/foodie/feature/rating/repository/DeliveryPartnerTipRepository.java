package com.pvc.foodie.feature.rating.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pvc.foodie.feature.rating.entity.DeliveryPartnerTip;

public interface DeliveryPartnerTipRepository extends JpaRepository<DeliveryPartnerTip, UUID> {

    boolean existsByOrderIdAndDeliveryPartnerId(UUID orderId, UUID deliveryPartnerId);

    Optional<DeliveryPartnerTip> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    List<DeliveryPartnerTip> findByDeliveryPartnerIdOrderByCreatedAtDesc(UUID deliveryPartnerId);

    @Query("select coalesce(sum(t.amount), 0) from DeliveryPartnerTip t where t.deliveryPartner.id = :deliveryPartnerId")
    BigDecimal sumAmountByDeliveryPartnerId(@Param("deliveryPartnerId") UUID deliveryPartnerId);

    @Query("select coalesce(sum(t.amount), 0) from DeliveryPartnerTip t where t.order.id = :orderId")
    BigDecimal sumAmountByOrderId(@Param("orderId") UUID orderId);
}
