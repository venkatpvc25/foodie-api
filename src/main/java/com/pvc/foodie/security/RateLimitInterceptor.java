package com.pvc.foodie.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.config.RateLimitProperties;
import com.pvc.foodie.config.RateLimitProperties.Rule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        Rule rule = ruleFor(request.getRequestURI());
        if (rule == null) {
            return true;
        }

        String clientIp = clientIp(request);
        String key = "ip:" + clientIp + ":path:" + request.getRequestURI();
        if (!rateLimitService.allow(key, rule)) {
            log.warn("Rate limit exceeded: clientIp={}, method={}, path={}",
                    clientIp, request.getMethod(), request.getRequestURI());
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Too many requests. Please try again later.");
        }
        return true;
    }

    private Rule ruleFor(String path) {
        if ("/auth/login".equals(path)) {
            return properties.getLogin();
        }
        if ("/auth/refresh".equals(path)) {
            return properties.getRefresh();
        }
        if ("/auth/register".equals(path) || path.startsWith("/auth/signup/")) {
            return properties.getSignup();
        }
        if ("/orders/guest-checkout/otp".equals(path)) {
            return properties.getOtpIp();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
