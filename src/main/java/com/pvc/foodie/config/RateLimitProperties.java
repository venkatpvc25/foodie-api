package com.pvc.foodie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    private Rule login = new Rule(10, 60);
    private Rule refresh = new Rule(30, 60);
    private Rule signup = new Rule(5, 3600);
    private Rule otpIp = new Rule(5, 900);
    private Rule otpPhone = new Rule(5, 900);

    @Getter
    @Setter
    public static class Rule {
        private int maxRequests;
        private int windowSeconds;

        public Rule() {
        }

        public Rule(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
