package com.pvc.foodie.feature.cart.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.foodie.feature.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByIdAndCartUserId(UUID id, UUID userId);
}
