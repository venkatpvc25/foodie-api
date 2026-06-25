package com.pvc.foodie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.firebase")
public record FirebaseProperties(
        boolean enabled,
        String serviceAccountPath,
        String appName) {
}
