package com.pvc.foodie.feature.order.dto;

import java.util.List;

import com.pvc.foodie.feature.address.dto.AddressRequest;
import com.pvc.foodie.feature.cart.dto.CartItemRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestCheckoutRequest(
                @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be a valid phone number") String phone,
                @Valid @NotNull AddressRequest address,
                @Valid @NotEmpty List<CartItemRequest> items,
                @Size(max = 1000) String notes) {
}
