package com.pvc.foodie.feature.order.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.order.dto.DeliveryDashboardResponse;
import com.pvc.foodie.feature.order.dto.GuestCheckoutRequest;
import com.pvc.foodie.feature.order.dto.GuestCheckoutResponse;
import com.pvc.foodie.feature.order.dto.OrderResponse;
import com.pvc.foodie.feature.order.dto.PlaceOrderRequest;
import com.pvc.foodie.feature.order.dto.UpdateOrderStatusRequest;
import com.pvc.foodie.feature.order.dto.VerifyRazorpayPaymentRequest;
import com.pvc.foodie.feature.order.service.OrderService;
import com.pvc.foodie.feature.order.service.RazorpayPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RazorpayPaymentService razorpayPaymentService;

    @PostMapping
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(orderService.placeOrder(request));
    }

    @PostMapping("/guest-checkout")
    public ApiResponse<GuestCheckoutResponse> placeGuestOrder(@Valid @RequestBody GuestCheckoutRequest request) {
        return ApiResponse.ok(orderService.placeGuestOrder(request));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders() {
        return ApiResponse.ok(orderService.getOrders());
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.getOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.cancelOrder(id));
    }

    @PostMapping("/{id}/payment/razorpay/order")
    public ApiResponse<OrderResponse> createRazorpayOrder(@PathVariable UUID id) {
        return ApiResponse.ok(razorpayPaymentService.createRazorpayOrder(id));
    }

    @PostMapping("/{id}/payment/razorpay/verify")
    public ApiResponse<OrderResponse> verifyRazorpayPayment(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyRazorpayPaymentRequest request) {
        return ApiResponse.ok(razorpayPaymentService.verifyRazorpayPayment(id, request));
    }

    @GetMapping("/restaurant")
    public ApiResponse<List<OrderResponse>> getRestaurantOrders() {
        return ApiResponse.ok(orderService.getRestaurantOrders());
    }

    @PostMapping("/restaurant/{id}/status")
    public ApiResponse<OrderResponse> updateRestaurantOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateRestaurantOrderStatus(id, request.status()));
    }

    @GetMapping("/delivery/available")
    public ApiResponse<List<OrderResponse>> getAvailableDeliveryOrders() {
        return ApiResponse.ok(orderService.getAvailableDeliveryOrders());
    }

    @GetMapping("/delivery/my")
    public ApiResponse<List<OrderResponse>> getMyDeliveryOrders() {
        return ApiResponse.ok(orderService.getMyDeliveryOrders());
    }

    @GetMapping("/delivery/dashboard")
    public ApiResponse<DeliveryDashboardResponse> getDeliveryDashboard() {
        return ApiResponse.ok(orderService.getDeliveryDashboard());
    }

    @PostMapping("/delivery/{id}/claim")
    public ApiResponse<OrderResponse> claimDeliveryOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.claimDeliveryOrder(id));
    }

    @PostMapping("/delivery/{id}/pickup")
    public ApiResponse<OrderResponse> pickupDeliveryOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.pickupDeliveryOrder(id));
    }

    @PostMapping("/delivery/{id}/delivered")
    public ApiResponse<OrderResponse> deliverDeliveryOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.deliverDeliveryOrder(id));
    }

    @PostMapping("/delivery/{id}/status")
    public ApiResponse<OrderResponse> updateDeliveryOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateDeliveryOrderStatus(id, request.status()));
    }
}
