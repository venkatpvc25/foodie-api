package com.pvc.foodie.feature.rating.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pvc.foodie.feature.rating.entity.DeliveryPartnerRating;

public interface DeliveryPartnerRatingRepository extends JpaRepository<DeliveryPartnerRating, UUID> {

    boolean existsByOrderIdAndDeliveryPartnerId(UUID orderId, UUID deliveryPartnerId);

    Optional<DeliveryPartnerRating> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    List<DeliveryPartnerRating> findByDeliveryPartnerIdOrderByCreatedAtDesc(UUID deliveryPartnerId);

    @Query("select coalesce(avg(r.rating), 0), count(r) from DeliveryPartnerRating r where r.deliveryPartner.id = :deliveryPartnerId")
    Object[] getSummary(@Param("deliveryPartnerId") UUID deliveryPartnerId);
}
