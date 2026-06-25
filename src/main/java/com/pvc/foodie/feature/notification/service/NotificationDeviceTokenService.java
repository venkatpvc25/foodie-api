package com.pvc.foodie.feature.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.notification.dto.DeviceTokenRequest;
import com.pvc.foodie.feature.notification.entity.NotificationDeviceToken;
import com.pvc.foodie.feature.notification.repository.NotificationDeviceTokenRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeviceTokenService {

    private final NotificationDeviceTokenRepository deviceTokenRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void registerDeviceToken(DeviceTokenRequest request) {
        User user = currentUserService.getCurrentUser();
        NotificationDeviceToken deviceToken = deviceTokenRepository.findByToken(request.token())
                .orElseGet(NotificationDeviceToken::new);
        deviceToken.setUser(user);
        deviceToken.setToken(request.token());
        deviceToken.setPlatform(request.platform());
        NotificationDeviceToken saved = deviceTokenRepository.save(deviceToken);
        log.info("Notification device token registered: userId={}, deviceTokenId={}, platform={}",
                user.getId(), saved.getId(), saved.getPlatform());
    }

    @Transactional
    public void deleteDeviceToken(DeviceTokenRequest request) {
        User user = currentUserService.getCurrentUser();
        deviceTokenRepository.findByUserIdAndToken(user.getId(), request.token())
                .ifPresent(deviceToken -> {
                    deviceTokenRepository.delete(deviceToken);
                    log.info("Notification device token deleted: userId={}, deviceTokenId={}",
                            user.getId(), deviceToken.getId());
                });
    }
}
