package com.pvc.foodie.feature.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminRestaurantResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        String phone,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean open,
        boolean approved,
        boolean suspended,
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        String razorpayLinkedAccountId,
        boolean razorpayReady,
        BigDecimal commissionRate) {
}
