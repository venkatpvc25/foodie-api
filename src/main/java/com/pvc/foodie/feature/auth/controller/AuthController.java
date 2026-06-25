package com.pvc.foodie.feature.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.auth.dto.AuthResponse;
import com.pvc.foodie.feature.auth.dto.CurrentUserResponse;
import com.pvc.foodie.feature.auth.dto.CustomerSignupRequest;
import com.pvc.foodie.feature.auth.dto.LoginRequest;
import com.pvc.foodie.feature.auth.dto.RefreshTokenRequest;
import com.pvc.foodie.feature.auth.service.AuthService;

import jakarta.validation.Valid;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody CustomerSignupRequest request) {
        return ApiResponse.ok(service.register(request));
    }

    @PostMapping("/signup/customer")
    public ApiResponse<AuthResponse> signupCustomer(@Valid @RequestBody CustomerSignupRequest request) {
        return ApiResponse.ok(service.signupCustomer(request));
    }

    @PostMapping("/signup/restaurant")
    public ApiResponse<AuthResponse> signupRestaurant(@Valid @RequestBody CustomerSignupRequest request) {
        return ApiResponse.ok(service.signupRestaurant(request));
    }

    @PostMapping("/signup/delivery-partner")
    public ApiResponse<AuthResponse> signupDeliveryPartner(@Valid @RequestBody CustomerSignupRequest request) {
        return ApiResponse.ok(service.signupDeliveryPartner(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(service.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(service.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        service.logout(request.refreshToken());
        return ApiResponse.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(Principal principal) {
        return ApiResponse.ok(service.getCurrentUser(principal.getName()));
    }
}
