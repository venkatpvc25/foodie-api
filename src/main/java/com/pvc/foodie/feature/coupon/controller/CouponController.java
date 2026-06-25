package com.pvc.foodie.feature.coupon.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.coupon.dto.CouponPreviewRequest;
import com.pvc.foodie.feature.coupon.dto.CouponPreviewResponse;
import com.pvc.foodie.feature.coupon.dto.CouponRequest;
import com.pvc.foodie.feature.coupon.dto.CouponResponse;
import com.pvc.foodie.feature.coupon.service.CouponService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ApiResponse<List<CouponResponse>> getCoupons(@RequestParam(required = false) UUID restaurantId) {
        return ApiResponse.ok(couponService.getCoupons(restaurantId));
    }

    @PostMapping
    public ApiResponse<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(couponService.createCoupon(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> updateCoupon(
            @PathVariable UUID id,
            @Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteCoupon(@PathVariable UUID id) {
        couponService.deleteCoupon(id);
        return ApiResponse.ok("Coupon deleted successfully");
    }

    @PostMapping("/preview")
    public ApiResponse<CouponPreviewResponse> previewCoupon(@Valid @RequestBody CouponPreviewRequest request) {
        return ApiResponse.ok(couponService.previewCoupon(request));
    }
}
