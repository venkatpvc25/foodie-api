package com.pvc.foodie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.guest-checkout.otp")
@Getter
@Setter
public class GuestCheckoutOtpProperties {

    private int ttlSeconds = 300;
    private int maxAttempts = 5;
    private boolean debugResponseEnabled;
}
