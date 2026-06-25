package com.pvc.foodie.feature.cart.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.cart.dto.CartItemRequest;
import com.pvc.foodie.feature.cart.dto.CartResponse;
import com.pvc.foodie.feature.cart.dto.UpdateCartItemRequest;
import com.pvc.foodie.feature.cart.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.ok(cartService.getCart());
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody CartItemRequest request) {
        return ApiResponse.ok(cartService.addItem(request));
    }

    @PatchMapping("/items/{id}")
    public ApiResponse<CartResponse> updateItem(@PathVariable UUID id,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.ok(cartService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<String> removeItem(@PathVariable UUID id) {
        cartService.removeItem(id);
        return ApiResponse.ok("Cart item removed successfully");
    }

    @DeleteMapping
    public ApiResponse<String> clearCart() {
        cartService.clearCart();
        return ApiResponse.ok("Cart cleared successfully");
    }
}
