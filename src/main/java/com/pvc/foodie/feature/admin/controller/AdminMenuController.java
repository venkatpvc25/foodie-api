package com.pvc.foodie.feature.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.admin.dto.AdminMenuItemUpdateRequest;
import com.pvc.foodie.feature.admin.service.AdminMenuService;
import com.pvc.foodie.feature.restaurant.dto.BulkMenuUploadRequest;
import com.pvc.foodie.feature.restaurant.dto.BulkMenuUploadResponse;
import com.pvc.foodie.feature.restaurant.dto.MenuItemResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/restaurants/{restaurantId}/menu")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    @GetMapping
    public ApiResponse<List<MenuItemResponse>> getMenu(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean available) {
        return ApiResponse.ok(adminMenuService.getMenu(restaurantId, categoryId, available));
    }

    @PostMapping("/bulk")
    public ApiResponse<BulkMenuUploadResponse> bulkCreateMenuItems(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody BulkMenuUploadRequest request) {
        return ApiResponse.ok(adminMenuService.bulkCreateMenuItems(restaurantId, request));
    }

    @PatchMapping("/{menuItemId}")
    public ApiResponse<MenuItemResponse> updateMenuItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuItemId,
            @Valid @RequestBody AdminMenuItemUpdateRequest request) {
        return ApiResponse.ok(adminMenuService.updateMenuItem(restaurantId, menuItemId, request));
    }

    @PostMapping("/{menuItemId}/enable")
    public ApiResponse<MenuItemResponse> enableMenuItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuItemId) {
        return ApiResponse.ok(adminMenuService.setMenuItemAvailable(restaurantId, menuItemId, true));
    }

    @PostMapping("/{menuItemId}/disable")
    public ApiResponse<MenuItemResponse> disableMenuItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuItemId) {
        return ApiResponse.ok(adminMenuService.setMenuItemAvailable(restaurantId, menuItemId, false));
    }
}
