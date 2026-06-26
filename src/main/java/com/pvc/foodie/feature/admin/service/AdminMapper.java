package com.pvc.foodie.feature.admin.service;

import org.springframework.stereotype.Component;

import com.pvc.foodie.feature.admin.dto.AdminCouponResponse;
import com.pvc.foodie.feature.admin.dto.AdminRestaurantResponse;
import com.pvc.foodie.feature.admin.dto.AdminUserResponse;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.coupon.entity.Coupon;
import com.pvc.foodie.feature.restaurant.entity.Restaurant;

@Component
public class AdminMapper {

    public AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.isBlocked(),
                isValidRazorpayAccountId(user.getRazorpayLinkedAccountId()),
                user.getRazorpayLinkedAccountId());
    }

    public AdminRestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        User owner = restaurant.getCreatedBy();
        return new AdminRestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getImageUrl(),
                restaurant.getPhone(),
                restaurant.getAddress(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.isOpen(),
                restaurant.isApproved(),
                restaurant.isSuspended(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getName(),
                owner == null ? null : owner.getEmail(),
                restaurant.getRazorpayLinkedAccountId(),
                isValidRazorpayAccountId(restaurant.getRazorpayLinkedAccountId()),
                restaurant.getCommissionRate());
    }

    public AdminCouponResponse toCouponResponse(Coupon coupon) {
        return new AdminCouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinOrderAmount(),
                coupon.getRestaurant() == null ? null : coupon.getRestaurant().getId(),
                coupon.getRestaurant() == null ? null : coupon.getRestaurant().getName(),
                coupon.isActive(),
                coupon.getValidFrom(),
                coupon.getValidTo(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getCreatedBy() == null ? null : coupon.getCreatedBy().getId(),
                coupon.getCreatedBy() == null ? null : coupon.getCreatedBy().getName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isValidRazorpayAccountId(String value) {
        return hasText(value) && value.startsWith("acc_");
    }
}
