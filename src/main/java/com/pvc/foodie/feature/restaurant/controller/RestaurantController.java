package com.pvc.foodie.feature.restaurant.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.restaurant.dto.BulkMenuUploadRequest;
import com.pvc.foodie.feature.restaurant.dto.BulkMenuUploadResponse;
import com.pvc.foodie.feature.restaurant.dto.CategoryRequest;
import com.pvc.foodie.feature.restaurant.dto.CategoryResponse;
import com.pvc.foodie.feature.restaurant.dto.MenuItemRequest;
import com.pvc.foodie.feature.restaurant.dto.MenuItemResponse;
import com.pvc.foodie.feature.restaurant.dto.RestaurantCommissionRequest;
import com.pvc.foodie.feature.restaurant.dto.RestaurantRequest;
import com.pvc.foodie.feature.restaurant.dto.RestaurantResponse;
import com.pvc.foodie.feature.restaurant.service.RestaurantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    public ApiResponse<List<RestaurantResponse>> getRestaurants(@RequestParam(required = false) String search) {
        return ApiResponse.ok(restaurantService.getRestaurants(search));
    }

    @GetMapping("/my")
    public ApiResponse<List<RestaurantResponse>> getMyRestaurants() {
        return ApiResponse.ok(restaurantService.getMyRestaurants());
    }

    @PostMapping
    public ApiResponse<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return ApiResponse.ok(restaurantService.createRestaurant(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantResponse> getRestaurant(@PathVariable UUID id) {
        return ApiResponse.ok(restaurantService.getRestaurant(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantResponse> updateRestaurant(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantRequest request) {
        return ApiResponse.ok(restaurantService.updateRestaurant(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteRestaurant(@PathVariable UUID id) {
        restaurantService.deleteRestaurant(id);
        return ApiResponse.ok("Restaurant deleted successfully");
    }

    @PatchMapping("/{id}/commission")
    public ApiResponse<RestaurantResponse> updateRestaurantCommission(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantCommissionRequest request) {
        return ApiResponse.ok(restaurantService.updateRestaurantCommission(id, request));
    }

    @GetMapping("/{id}/categories")
    public ApiResponse<List<CategoryResponse>> getCategories(@PathVariable UUID id) {
        return ApiResponse.ok(restaurantService.getCategories(id));
    }

    @PostMapping("/{id}/categories")
    public ApiResponse<CategoryResponse> createCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(restaurantService.createCategory(id, request));
    }

    @PutMapping("/{id}/categories/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(restaurantService.updateCategory(id, categoryId, request));
    }

    @DeleteMapping("/{id}/categories/{categoryId}")
    public ApiResponse<?> deleteCategory(@PathVariable UUID id, @PathVariable UUID categoryId) {
        restaurantService.deleteCategory(id, categoryId);
        return ApiResponse.ok("Category deleted successfully");
    }

    @GetMapping("/{id}/menu")
    public ApiResponse<List<MenuItemResponse>> getMenu(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID categoryId) {
        return ApiResponse.ok(restaurantService.getMenu(id, categoryId));
    }

    @PostMapping("/{id}/menu")
    public ApiResponse<MenuItemResponse> createMenuItem(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(restaurantService.createMenuItem(id, request));
    }

    @PostMapping("/{id}/menu/bulk")
    public ApiResponse<BulkMenuUploadResponse> bulkCreateMenuItems(
            @PathVariable UUID id,
            @Valid @RequestBody BulkMenuUploadRequest request) {
        return ApiResponse.ok(restaurantService.bulkCreateMenuItems(id, request));
    }

    @PutMapping("/{id}/menu/{menuItemId}")
    public ApiResponse<MenuItemResponse> updateMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID menuItemId,
            @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(restaurantService.updateMenuItem(id, menuItemId, request));
    }

    @DeleteMapping("/{id}/menu/{menuItemId}")
    public ApiResponse<?> deleteMenuItem(@PathVariable UUID id, @PathVariable UUID menuItemId) {
        restaurantService.deleteMenuItem(id, menuItemId);
        return ApiResponse.ok("Menu item deleted successfully");
    }
}
