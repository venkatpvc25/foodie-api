package com.pvc.foodie.feature.notification.service;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.notification.entity.Notification;
import com.pvc.foodie.feature.notification.entity.NotificationDeviceToken;
import com.pvc.foodie.feature.notification.repository.NotificationDeviceTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebasePushNotificationService {

    private final NotificationDeviceTokenRepository deviceTokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public void sendToUser(User user, Notification notification) {
        if (user == null) {
            return;
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.debug("Firebase push skipped because Firebase is disabled: userId={}", user.getId());
            return;
        }

        List<NotificationDeviceToken> deviceTokens = deviceTokenRepository.findByUserId(user.getId());
        if (deviceTokens.isEmpty()) {
            log.debug("Firebase push skipped because user has no device tokens: userId={}", user.getId());
            return;
        }

        for (NotificationDeviceToken deviceToken : deviceTokens) {
            sendToDevice(firebaseMessaging, user, deviceToken, notification);
        }
    }

    private void sendToDevice(
            FirebaseMessaging firebaseMessaging,
            User user,
            NotificationDeviceToken deviceToken,
            Notification notification) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken.getToken())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getMessage())
                            .build())
                    .putData("notificationId", notification.getId().toString())
                    .putData("title", notification.getTitle())
                    .putData("message", notification.getMessage());

            putDataIfPresent(messageBuilder, "type", notification.getType());
            putDataIfPresent(messageBuilder, "targetType", notification.getTargetType());
            putDataIfPresent(messageBuilder, "targetId", notification.getTargetId() == null ? null : notification.getTargetId().toString());
            putDataIfPresent(messageBuilder, "route", notification.getRoute());

            String messageId = firebaseMessaging.send(messageBuilder.build());
            log.info("Firebase push sent: userId={}, deviceTokenId={}, messageId={}",
                    user.getId(), deviceToken.getId(), messageId);
        } catch (FirebaseMessagingException ex) {
            if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                deviceTokenRepository.delete(deviceToken);
                log.info("Invalid Firebase device token removed: userId={}, deviceTokenId={}, errorCode={}",
                        user.getId(), deviceToken.getId(), ex.getMessagingErrorCode());
                return;
            }
            log.warn("Firebase push failed: userId={}, deviceTokenId={}, errorCode={}",
                    user.getId(), deviceToken.getId(), ex.getMessagingErrorCode(), ex);
        } catch (Exception ex) {
            log.warn("Firebase push failed: userId={}, deviceTokenId={}",
                    user.getId(), deviceToken.getId(), ex);
        }
    }

    private void putDataIfPresent(Message.Builder messageBuilder, String key, String value) {
        if (value != null && !value.isBlank()) {
            messageBuilder.putData(key, value);
        }
    }
}
