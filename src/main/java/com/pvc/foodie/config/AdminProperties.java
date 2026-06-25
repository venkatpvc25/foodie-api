package com.pvc.foodie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.admin")
@Getter
@Setter
public class AdminProperties {

    private boolean enabled;
    private String email;
    private String password;
}
