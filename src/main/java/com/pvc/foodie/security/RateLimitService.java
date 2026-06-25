package com.pvc.foodie.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.pvc.foodie.config.RateLimitProperties;
import com.pvc.foodie.config.RateLimitProperties.Rule;

@Service
public class RateLimitService {

    private final RateLimitProperties properties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public boolean allow(String key, Rule rule) {
        if (!properties.isEnabled()) {
            return true;
        }

        long now = Instant.now().getEpochSecond();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now, 0));

        synchronized (window) {
            if (now - window.startedAt >= rule.getWindowSeconds()) {
                window.startedAt = now;
                window.count = 0;
            }
            if (window.count >= rule.getMaxRequests()) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    private static class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
