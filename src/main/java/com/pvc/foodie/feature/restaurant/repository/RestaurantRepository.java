package com.pvc.foodie.feature.restaurant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pvc.foodie.feature.restaurant.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    List<Restaurant> findByNameContainingIgnoreCase(String search);

    List<Restaurant> findByCreatedByIdOrderByNameAsc(UUID userId);

    Optional<Restaurant> findByIdAndCreatedById(UUID id, UUID userId);

    @Query("select r.razorpayLinkedAccountId from Restaurant r where r.id = :id")
    Optional<String> findRazorpayLinkedAccountIdById(@Param("id") UUID id);
}
