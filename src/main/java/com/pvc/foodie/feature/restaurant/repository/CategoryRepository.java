package com.pvc.foodie.feature.restaurant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.restaurant.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByRestaurantIdOrderByDisplayOrderAsc(UUID restaurantId);

    Optional<Category> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
