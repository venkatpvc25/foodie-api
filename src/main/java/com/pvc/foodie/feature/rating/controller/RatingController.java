package com.pvc.foodie.feature.rating.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.rating.dto.DeliveryPartnerRatingResponse;
import com.pvc.foodie.feature.rating.dto.DeliveryTipRequest;
import com.pvc.foodie.feature.rating.dto.DeliveryTipResponse;
import com.pvc.foodie.feature.rating.dto.MenuItemRatingResponse;
import com.pvc.foodie.feature.rating.dto.RatingRequest;
import com.pvc.foodie.feature.rating.dto.RatingSummary;
import com.pvc.foodie.feature.rating.dto.RestaurantRatingResponse;
import com.pvc.foodie.feature.rating.service.RatingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/orders/{orderId}/restaurant")
    public ApiResponse<RestaurantRatingResponse> rateRestaurant(
            @PathVariable UUID orderId,
            @Valid @RequestBody RatingRequest request) {
        return ApiResponse.ok(ratingService.rateRestaurant(orderId, request));
    }

    @PostMapping("/orders/{orderId}/menu-items/{menuItemId}")
    public ApiResponse<MenuItemRatingResponse> rateMenuItem(
            @PathVariable UUID orderId,
            @PathVariable UUID menuItemId,
            @Valid @RequestBody RatingRequest request) {
        return ApiResponse.ok(ratingService.rateMenuItem(orderId, menuItemId, request));
    }

    @PostMapping("/orders/{orderId}/delivery-partner")
    public ApiResponse<DeliveryPartnerRatingResponse> rateDeliveryPartner(
            @PathVariable UUID orderId,
            @Valid @RequestBody RatingRequest request) {
        return ApiResponse.ok(ratingService.rateDeliveryPartner(orderId, request));
    }

    @PostMapping("/orders/{orderId}/delivery-partner/tip")
    public ApiResponse<DeliveryTipResponse> tipDeliveryPartner(
            @PathVariable UUID orderId,
            @Valid @RequestBody DeliveryTipRequest request) {
        return ApiResponse.ok(ratingService.tipDeliveryPartner(orderId, request));
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ApiResponse<List<RestaurantRatingResponse>> getRestaurantRatings(@PathVariable UUID restaurantId) {
        return ApiResponse.ok(ratingService.getRestaurantRatings(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/summary")
    public ApiResponse<RatingSummary> getRestaurantRatingSummary(@PathVariable UUID restaurantId) {
        return ApiResponse.ok(ratingService.getRestaurantRatingSummary(restaurantId));
    }

    @GetMapping("/menu-items/{menuItemId}")
    public ApiResponse<List<MenuItemRatingResponse>> getMenuItemRatings(@PathVariable UUID menuItemId) {
        return ApiResponse.ok(ratingService.getMenuItemRatings(menuItemId));
    }

    @GetMapping("/menu-items/{menuItemId}/summary")
    public ApiResponse<RatingSummary> getMenuItemRatingSummary(@PathVariable UUID menuItemId) {
        return ApiResponse.ok(ratingService.getMenuItemRatingSummary(menuItemId));
    }

    @GetMapping("/delivery-partners/{deliveryPartnerId}")
    public ApiResponse<List<DeliveryPartnerRatingResponse>> getDeliveryPartnerRatings(
            @PathVariable UUID deliveryPartnerId) {
        return ApiResponse.ok(ratingService.getDeliveryPartnerRatings(deliveryPartnerId));
    }

    @GetMapping("/delivery-partners/{deliveryPartnerId}/summary")
    public ApiResponse<RatingSummary> getDeliveryPartnerRatingSummary(@PathVariable UUID deliveryPartnerId) {
        return ApiResponse.ok(ratingService.getDeliveryPartnerRatingSummary(deliveryPartnerId));
    }

    @GetMapping("/delivery-partners/{deliveryPartnerId}/tips")
    public ApiResponse<List<DeliveryTipResponse>> getDeliveryPartnerTips(@PathVariable UUID deliveryPartnerId) {
        return ApiResponse.ok(ratingService.getDeliveryPartnerTips(deliveryPartnerId));
    }
}
