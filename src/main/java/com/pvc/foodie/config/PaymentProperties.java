package com.pvc.foodie.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments")
public record PaymentProperties(
        BigDecimal commissionRate,
        String adminRazorpayAccountId) {

    public BigDecimal commissionRate() {
        return commissionRate == null ? BigDecimal.ZERO : commissionRate;
    }
}
