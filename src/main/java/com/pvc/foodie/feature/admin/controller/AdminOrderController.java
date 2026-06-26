package com.pvc.foodie.feature.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.admin.dto.AdminOrderDetailResponse;
import com.pvc.foodie.feature.admin.dto.AdminOrderUpdateRequest;
import com.pvc.foodie.feature.admin.service.AdminOrderService;
import com.pvc.foodie.feature.order.dto.OrderResponse;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID deliveryPartnerId) {
        return ApiResponse.ok(adminOrderService.getOrders(
                status,
                paymentStatus,
                restaurantId,
                customerId,
                deliveryPartnerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminOrderDetailResponse> getOrder(@PathVariable UUID id) {
        return ApiResponse.ok(adminOrderService.getOrder(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<OrderResponse> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody AdminOrderUpdateRequest request) {
        return ApiResponse.ok(adminOrderService.updateOrder(id, request));
    }
}
