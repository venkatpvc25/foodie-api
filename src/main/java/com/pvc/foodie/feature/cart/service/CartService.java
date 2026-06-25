package com.pvc.foodie.feature.cart.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.cart.dto.CartItemRequest;
import com.pvc.foodie.feature.cart.dto.CartItemResponse;
import com.pvc.foodie.feature.cart.dto.CartResponse;
import com.pvc.foodie.feature.cart.dto.UpdateCartItemRequest;
import com.pvc.foodie.feature.cart.entity.Cart;
import com.pvc.foodie.feature.cart.entity.CartItem;
import com.pvc.foodie.feature.cart.repository.CartItemRepository;
import com.pvc.foodie.feature.cart.repository.CartRepository;
import com.pvc.foodie.feature.restaurant.entity.MenuItem;
import com.pvc.foodie.feature.restaurant.repository.MenuItemRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        CartResponse response = cartRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElseGet(() -> new CartResponse(null, null, null, java.util.List.of(), BigDecimal.ZERO));
        log.info("Cart fetched: userId={}, cartId={}, itemCount={}, subtotal={}",
                user.getId(), response.id(), response.items().size(), response.subtotal());
        return response;
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        MenuItem menuItem = menuItemRepository.findByIdAndAvailableTrue(request.menuItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Menu item not found"));
        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> createCart(user));

        clearCartWhenRestaurantChanges(user, cart, menuItem);

        cart.setRestaurant(menuItem.getRestaurant());
        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getMenuItem().getId().equals(menuItem.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setMenuItem(menuItem);
                    newItem.setPrice(menuItem.getPrice());
                    cart.getItems().add(newItem);
                    return newItem;
                });
        item.setQuantity(item.getQuantity() + request.quantity());
        Cart saved = cartRepository.save(cart);
        CartResponse response = toResponse(saved);
        log.info("Cart item added: userId={}, cartId={}, menuItemId={}, quantityAdded={}, itemQuantity={}, itemCount={}, subtotal={}",
                user.getId(), saved.getId(), menuItem.getId(), request.quantity(), item.getQuantity(),
                response.items().size(), response.subtotal());
        return response;
    }

    @Transactional
    public CartResponse updateItem(UUID id, UpdateCartItemRequest request) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        CartItem item = cartItemRepository.findByIdAndCartUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Cart item not found"));
        int oldQuantity = item.getQuantity();
        item.setQuantity(request.quantity());
        CartResponse response = toResponse(item.getCart());
        log.info("Cart item updated: userId={}, cartId={}, cartItemId={}, oldQuantity={}, newQuantity={}, subtotal={}",
                user.getId(), item.getCart().getId(), id, oldQuantity, request.quantity(), response.subtotal());
        return response;
    }

    @Transactional
    public void removeItem(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        CartItem item = cartItemRepository.findByIdAndCartUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Cart item not found"));
        Cart cart = item.getCart();
        cart.getItems().remove(item);
        if (cart.getItems().isEmpty()) {
            cart.setRestaurant(null);
        }
        log.info("Cart item removed: userId={}, cartId={}, cartItemId={}, remainingItemCount={}",
                user.getId(), cart.getId(), id, cart.getItems().size());
    }

    @Transactional
    public void clearCart() {
        User user = currentUserService.getCurrentUser();
        requireCustomer(user);
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            int itemCount = cart.getItems().size();
            cart.getItems().clear();
            cart.setRestaurant(null);
            log.info("Cart cleared: userId={}, cartId={}, clearedItemCount={}", user.getId(), cart.getId(), itemCount);
        });
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        log.info("Cart created: userId={}", user.getId());
        return cart;
    }

    private void clearCartWhenRestaurantChanges(User user, Cart cart, MenuItem menuItem) {
        if (!hasDifferentRestaurant(cart, menuItem)) {
            return;
        }
        log.info("Cart restaurant changed, clearing existing items: userId={}, cartId={}, oldRestaurantId={}, newRestaurantId={}, oldItemCount={}",
                user.getId(), cart.getId(), cart.getRestaurant().getId(), menuItem.getRestaurant().getId(),
                cart.getItems().size());
        cart.getItems().clear();
    }

    private boolean hasDifferentRestaurant(Cart cart, MenuItem menuItem) {
        return cart.getRestaurant() != null && !cart.getRestaurant().getId().equals(menuItem.getRestaurant().getId());
    }

    private CartResponse toResponse(Cart cart) {
        var items = cart.getItems().stream().map(this::toItemResponse).toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        UUID restaurantId = cart.getRestaurant() == null ? null : cart.getRestaurant().getId();
        String restaurantName = cart.getRestaurant() == null ? null : cart.getRestaurant().getName();
        return new CartResponse(cart.getId(), restaurantId, restaurantName, items, subtotal);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getMenuItem().getName(),
                item.getPrice(),
                item.getQuantity(),
                lineTotal);
    }

    private void requireCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.ADMIN) {
            log.warn("Cart access denied: userId={}, role={}", user.getId(), user.getRole());
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Customer access required");
        }
    }
}
