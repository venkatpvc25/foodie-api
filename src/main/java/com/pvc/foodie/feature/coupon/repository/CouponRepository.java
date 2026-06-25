package com.pvc.foodie.feature.coupon.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.coupon.entity.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findByRestaurantIdOrRestaurantIsNullOrderByCodeAsc(UUID restaurantId);

    List<Coupon> findByCreatedByIdOrderByCodeAsc(UUID createdById);
}
