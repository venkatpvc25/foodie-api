package com.pvc.foodie.feature.address.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AddressResponse(
                UUID id,
                String title,
                String addressLine1,
                String addressLine2,
                String city,
                String state,
                BigDecimal latitude,
                BigDecimal longitude,
                boolean defaultAddress) {
}
