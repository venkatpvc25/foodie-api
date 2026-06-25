package com.pvc.foodie.feature.rating.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pvc.foodie.feature.rating.entity.RestaurantRating;

public interface RestaurantRatingRepository extends JpaRepository<RestaurantRating, UUID> {

    boolean existsByOrderIdAndRestaurantId(UUID orderId, UUID restaurantId);

    Optional<RestaurantRating> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    List<RestaurantRating> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);

    @Query("select coalesce(avg(r.rating), 0), count(r) from RestaurantRating r where r.restaurant.id = :restaurantId")
    Object[] getSummary(@Param("restaurantId") UUID restaurantId);
}
