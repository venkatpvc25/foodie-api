package com.pvc.foodie.feature.restaurant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.restaurant.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByRestaurantIdOrderByDisplayOrderAsc(UUID restaurantId);

    List<MenuItem> findByRestaurantIdAndCategoryIdOrderByDisplayOrderAsc(UUID restaurantId, UUID categoryId);

    Optional<MenuItem> findByIdAndAvailableTrue(UUID id);

    Optional<MenuItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
