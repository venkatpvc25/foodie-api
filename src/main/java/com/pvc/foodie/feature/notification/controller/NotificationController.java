package com.pvc.foodie.feature.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.foodie.comman.response.ApiResponse;
import com.pvc.foodie.feature.notification.dto.DeviceTokenRequest;
import com.pvc.foodie.feature.notification.dto.NotificationResponse;
import com.pvc.foodie.feature.notification.service.NotificationDeviceTokenService;
import com.pvc.foodie.feature.notification.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDeviceTokenService deviceTokenService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications() {
        return ApiResponse.ok(notificationService.getNotifications());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @PostMapping("/device-tokens")
    public ApiResponse<String> registerDeviceToken(@Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.registerDeviceToken(request);
        return ApiResponse.ok("Device token registered");
    }

    @DeleteMapping("/device-tokens")
    public ApiResponse<String> deleteDeviceToken(@Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.deleteDeviceToken(request);
        return ApiResponse.ok("Device token deleted");
    }
}
