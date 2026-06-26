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
import com.pvc.foodie.feature.admin.dto.AdminRestaurantCreateRequest;
import com.pvc.foodie.feature.admin.dto.AdminRestaurantResponse;
import com.pvc.foodie.feature.admin.dto.AdminRestaurantUpdateRequest;
import com.pvc.foodie.feature.admin.service.AdminRestaurantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/restaurants")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final AdminRestaurantService adminRestaurantService;

    @GetMapping
    public ApiResponse<List<AdminRestaurantResponse>> getRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean open,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) Boolean suspended,
            @RequestParam(required = false) Boolean missingRazorpayAccount) {
        return ApiResponse.ok(adminRestaurantService.getRestaurants(
                search,
                open,
                approved,
                suspended,
                missingRazorpayAccount));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminRestaurantResponse> getRestaurant(@PathVariable UUID id) {
        return ApiResponse.ok(adminRestaurantService.getRestaurant(id));
    }

    @PostMapping
    public ApiResponse<AdminRestaurantResponse> createRestaurant(
            @Valid @RequestBody AdminRestaurantCreateRequest request) {
        return ApiResponse.ok(adminRestaurantService.createRestaurant(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminRestaurantResponse> updateRestaurant(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRestaurantUpdateRequest request) {
        return ApiResponse.ok(adminRestaurantService.updateRestaurant(id, request));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<AdminRestaurantResponse> approveRestaurant(@PathVariable UUID id) {
        return ApiResponse.ok(adminRestaurantService.approveRestaurant(id));
    }

    @PostMapping("/{id}/suspend")
    public ApiResponse<AdminRestaurantResponse> suspendRestaurant(@PathVariable UUID id) {
        return ApiResponse.ok(adminRestaurantService.suspendRestaurant(id));
    }

    @PostMapping("/{id}/unsuspend")
    public ApiResponse<AdminRestaurantResponse> unsuspendRestaurant(@PathVariable UUID id) {
        return ApiResponse.ok(adminRestaurantService.unsuspendRestaurant(id));
    }
}
