package com.pvc.foodie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.auth")
@Getter
@Setter
public class AuthProperties {

    private boolean restaurantSignupEnabled;
    private boolean deliveryPartnerSignupEnabled;
}
