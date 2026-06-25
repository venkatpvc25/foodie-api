package com.pvc.foodie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.pvc.foodie.security.RateLimitInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/register",
                        "/auth/signup/**",
                        "/orders/guest-checkout/otp");
    }
}
