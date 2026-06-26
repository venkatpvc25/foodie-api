package com.pvc.foodie.feature.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.admin.dto.AdminCouponResponse;
import com.pvc.foodie.feature.admin.service.AdminCouponService;
import com.pvc.foodie.feature.coupon.dto.CouponRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @GetMapping
    public ApiResponse<List<AdminCouponResponse>> getCoupons(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID restaurantId) {
        return ApiResponse.ok(adminCouponService.getCoupons(active, restaurantId));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminCouponResponse> getCoupon(@PathVariable UUID id) {
        return ApiResponse.ok(adminCouponService.getCoupon(id));
    }

    @PostMapping
    public ApiResponse<AdminCouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(adminCouponService.createCoupon(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCouponResponse> updateCoupon(
            @PathVariable UUID id,
            @Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(adminCouponService.updateCoupon(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<AdminCouponResponse> activateCoupon(@PathVariable UUID id) {
        return ApiResponse.ok(adminCouponService.setCouponActive(id, true));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<AdminCouponResponse> deactivateCoupon(@PathVariable UUID id) {
        return ApiResponse.ok(adminCouponService.setCouponActive(id, false));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteCoupon(@PathVariable UUID id) {
        adminCouponService.deleteCoupon(id);
        return ApiResponse.ok("Coupon deleted successfully");
    }
}
