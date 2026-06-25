package com.pvc.foodie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pvc.foodie.config.AdminProperties;
import com.pvc.foodie.config.PaymentProperties;
import com.pvc.foodie.config.RazorpayProperties;

@SpringBootApplication
@EnableConfigurationProperties({ AdminProperties.class, PaymentProperties.class, RazorpayProperties.class })
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableScheduling
public class FoodOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodOrderApplication.class, args);
    }
}
