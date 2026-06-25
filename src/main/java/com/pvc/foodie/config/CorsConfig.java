package com.pvc.foodie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

        @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
        private String allowedOrigins;

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isBlank())
                                .toList());

                config.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "DELETE", "PATCH"));

                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type"));

                config.setAllowCredentials(true);

                config.setExposedHeaders(List.of(
                                "Authorization"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", config);

                return source;
        }
}
