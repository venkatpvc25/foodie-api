package com.pvc.foodie.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.notifications.firebase", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        if (properties.serviceAccountPath() == null || properties.serviceAccountPath().isBlank()) {
            throw new IllegalStateException("Firebase service account path is required when Firebase is enabled");
        }

        String appName = properties.appName() == null || properties.appName().isBlank()
                ? "foodie"
                : properties.appName();
        List<FirebaseApp> existingApps = FirebaseApp.getApps();
        for (FirebaseApp app : existingApps) {
            if (appName.equals(app.getName())) {
                return app;
            }
        }

        try (FileInputStream serviceAccount = new FileInputStream(properties.serviceAccountPath())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options, appName);
            log.info("Firebase app initialized: appName={}", appName);
            return app;
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.notifications.firebase", name = "enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
