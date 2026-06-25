package com.pvc.foodie.feature.rating.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pvc.foodie.feature.rating.entity.MenuItemRating;

public interface MenuItemRatingRepository extends JpaRepository<MenuItemRating, UUID> {

    boolean existsByOrderIdAndMenuItemId(UUID orderId, UUID menuItemId);

    Optional<MenuItemRating> findByOrderIdAndMenuItemIdAndCustomerId(UUID orderId, UUID menuItemId, UUID customerId);

    List<MenuItemRating> findByMenuItemIdOrderByCreatedAtDesc(UUID menuItemId);

    @Query("select coalesce(avg(r.rating), 0), count(r) from MenuItemRating r where r.menuItem.id = :menuItemId")
    Object[] getSummary(@Param("menuItemId") UUID menuItemId);
}
