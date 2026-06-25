package com.pvc.foodie.feature.address.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
                @Size(max = 80) String title,
                @NotBlank @Size(max = 255) String addressLine1,
                @Size(max = 255) String addressLine2,
                @Size(max = 100) String city,
                @Size(max = 100) String state,
                BigDecimal latitude,
                BigDecimal longitude,
                boolean defaultAddress) {
}
