package com.pvc.foodie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments.razorpay")
public record RazorpayProperties(
        boolean enabled,
        String keyId,
        String keySecret,
        String currency) {

    public String currency() {
        return currency == null || currency.isBlank() ? "INR" : currency;
    }
}
