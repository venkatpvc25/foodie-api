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
import com.pvc.foodie.feature.admin.dto.AdminUserCreateRequest;
import com.pvc.foodie.feature.admin.dto.AdminUserResponse;
import com.pvc.foodie.feature.admin.dto.AdminUserUpdateRequest;
import com.pvc.foodie.feature.admin.service.AdminUserService;
import com.pvc.foodie.feature.auth.entity.Role;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) Boolean missingRazorpayAccount,
            @RequestParam(required = false) String search) {
        return ApiResponse.ok(adminUserService.getUsers(role, blocked, missingRazorpayAccount, search));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.ok(adminUserService.getUser(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminUserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.ok(adminUserService.updateUser(id, request));
    }

    @PostMapping("/restaurant-owners")
    public ApiResponse<AdminUserResponse> createRestaurantOwner(
            @Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.ok(adminUserService.createRestaurantOwner(request));
    }

    @PostMapping("/delivery-partners")
    public ApiResponse<AdminUserResponse> createDeliveryPartner(
            @Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.ok(adminUserService.createDeliveryPartner(request));
    }

    @PostMapping("/{id}/block")
    public ApiResponse<AdminUserResponse> blockUser(@PathVariable UUID id) {
        return ApiResponse.ok(adminUserService.blockUser(id));
    }

    @PostMapping("/{id}/unblock")
    public ApiResponse<AdminUserResponse> unblockUser(@PathVariable UUID id) {
        return ApiResponse.ok(adminUserService.unblockUser(id));
    }
}
