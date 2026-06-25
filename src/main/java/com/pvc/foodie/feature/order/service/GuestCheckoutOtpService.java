package com.pvc.foodie.feature.order.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.config.GuestCheckoutOtpProperties;
import com.pvc.foodie.config.RateLimitProperties;
import com.pvc.foodie.feature.order.dto.GuestCheckoutOtpResponse;
import com.pvc.foodie.security.RateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestCheckoutOtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final GuestCheckoutOtpProperties properties;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitService rateLimitService;
    private final Map<String, OtpChallenge> challenges = new ConcurrentHashMap<>();

    public GuestCheckoutOtpResponse requestOtp(String phone) {
        String normalizedPhone = normalize(phone);
        requireOtpRequestAllowed(normalizedPhone);
        String code = String.valueOf(100000 + RANDOM.nextInt(900000));
        Instant expiresAt = Instant.now().plusSeconds(properties.getTtlSeconds());
        challenges.put(normalizedPhone, new OtpChallenge(code, expiresAt, 0));
        log.info("Guest checkout OTP generated: phone={}, expiresAt={}",
                normalizedPhone, expiresAt);
        String debugCode = properties.isDebugResponseEnabled() ? code : null;
        return new GuestCheckoutOtpResponse(normalizedPhone, properties.getTtlSeconds(), debugCode);
    }

    public void verify(String phone, String code) {
        String normalizedPhone = normalize(phone);
        OtpChallenge challenge = challenges.get(normalizedPhone);
        requireChallengePresent(challenge);
        requireChallengeActive(normalizedPhone, challenge);
        requireAttemptsAvailable(normalizedPhone, challenge);
        if (!isMatchingCode(challenge, code)) {
            challenges.put(normalizedPhone, challenge.nextAttempt());
            throw invalidOtp("Invalid verification code");
        }
        challenges.remove(normalizedPhone);
        log.info("Guest checkout OTP verified: phone={}", normalizedPhone);
    }

    private void requireOtpRequestAllowed(String normalizedPhone) {
        if (!rateLimitService.allow("otp-phone:" + normalizedPhone, rateLimitProperties.getOtpPhone())) {
            log.warn("Guest checkout OTP phone rate limit exceeded: phone={}", normalizedPhone);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Too many OTP requests. Please try again later.");
        }
    }

    private void requireChallengePresent(OtpChallenge challenge) {
        if (challenge == null) {
            throw invalidOtp("Verification code is required");
        }
    }

    private void requireChallengeActive(String normalizedPhone, OtpChallenge challenge) {
        if (challenge.expiresAt().isBefore(Instant.now())) {
            challenges.remove(normalizedPhone);
            throw invalidOtp("Verification code expired");
        }
    }

    private void requireAttemptsAvailable(String normalizedPhone, OtpChallenge challenge) {
        if (challenge.attempts() >= properties.getMaxAttempts()) {
            challenges.remove(normalizedPhone);
            throw invalidOtp("Too many verification attempts");
        }
    }

    private boolean isMatchingCode(OtpChallenge challenge, String code) {
        return challenge.code().equals(code);
    }

    private BusinessException invalidOtp(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.trim();
    }

    private record OtpChallenge(String code, Instant expiresAt, int attempts) {
        private OtpChallenge nextAttempt() {
            return new OtpChallenge(code, expiresAt, attempts + 1);
        }
    }
}
