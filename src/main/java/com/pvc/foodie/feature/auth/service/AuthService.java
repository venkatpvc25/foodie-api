
package com.pvc.foodie.feature.auth.service;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.dto.AuthResponse;
import com.pvc.foodie.feature.auth.dto.CurrentUserResponse;
import com.pvc.foodie.feature.auth.dto.CustomerSignupRequest;
import com.pvc.foodie.feature.auth.dto.LoginRequest;
import com.pvc.foodie.feature.auth.entity.RefreshToken;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.auth.repository.RefreshTokenRepository;
import com.pvc.foodie.feature.auth.repository.UserRepository;
import com.pvc.foodie.feature.notification.event.UserSignedUpEvent;
import com.pvc.foodie.security.JwtService;

import java.time.Instant;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public AuthResponse register(CustomerSignupRequest request) {
                ensureEmailAvailable(request.getEmail());
                ensurePhoneAvailable(request.getPhone());

                User user = createUser(request.getName(), request.getPhone(), request.getEmail(), request.getPassword(),
                                Role.CUSTOMER);
                log.info("Customer registered: userId={}, email={}", user.getId(), user.getEmail());
                eventPublisher.publishEvent(new UserSignedUpEvent(user));

                return issueTokens(user);
        }

        @Transactional
        public AuthResponse signupCustomer(CustomerSignupRequest request) {
                return register(request);
        }

        @Transactional
        public AuthResponse signupRestaurant(CustomerSignupRequest request) {
                ensureEmailAvailable(request.getEmail());
                ensurePhoneAvailable(request.getPhone());

                User user = createUser(request.getName(), request.getPhone(), request.getEmail(), request.getPassword(),
                                Role.RESTAURANT);
                log.info("Restaurant user registered: userId={}, email={}", user.getId(), user.getEmail());
                eventPublisher.publishEvent(new UserSignedUpEvent(user));

                return issueTokens(user);
        }

        @Transactional
        public AuthResponse signupDeliveryPartner(CustomerSignupRequest request) {
                ensureEmailAvailable(request.getEmail());
                ensurePhoneAvailable(request.getPhone());

                User user = createUser(request.getName(), request.getPhone(), request.getEmail(), request.getPassword(),
                                Role.DELIVERY_PARTNER);
                user.setRazorpayLinkedAccountId(request.getRazorpayLinkedAccountId());
                log.info("Delivery partner registered: userId={}, email={}", user.getId(), user.getEmail());
                eventPublisher.publishEvent(new UserSignedUpEvent(user));

                return issueTokens(user);
        }

        public AuthResponse login(LoginRequest request) {

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> {
                                        log.warn("Login failed, email not found: email={}", request.getEmail());
                                        return new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                                                        "Invalid credentials");
                                });

                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        log.warn("Login failed, invalid password: userId={}, email={}", user.getId(), user.getEmail());
                        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
                }

                log.info("Login successful: userId={}, email={}, role={}", user.getId(), user.getEmail(),
                                user.getRole());
                return issueTokens(user);
        }

        public AuthResponse refresh(String token) {

                RefreshToken stored = refreshTokenRepository.findByToken(token)
                                .orElseThrow(() -> {
                                        log.warn("Refresh token rejected, token not found");
                                        return new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                                                        "Invalid refresh token");
                                });

                if (stored.isRevoked()) {
                        log.warn("Refresh token rejected, token revoked: userId={}", stored.getUser().getId());
                        throw new BusinessException(ErrorCode.TOKEN_REVOKED, "Token revoked");
                }

                if (stored.getExpiryDate().isBefore(Instant.now())) {
                        log.warn("Refresh token rejected, token expired: userId={}, expiryDate={}",
                                        stored.getUser().getId(), stored.getExpiryDate());
                        throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Token expired");
                }

                String newAccess = jwtService.generateAccessToken(
                                stored.getUser().getEmail());

                log.info("Access token refreshed: userId={}, email={}", stored.getUser().getId(),
                                stored.getUser().getEmail());
                return buildAuthResponse(stored.getUser(), newAccess, token);
        }

        public void logout(String token) {

                RefreshToken stored = refreshTokenRepository.findByToken(token)
                                .orElseThrow(() -> {
                                        log.warn("Logout failed, refresh token not found");
                                        return new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                                                        "Invalid refresh token");
                                });

                stored.setRevoked(true);
                refreshTokenRepository.save(stored);
                log.info("Logout successful, refresh token revoked: userId={}", stored.getUser().getId());
        }

        public CurrentUserResponse getCurrentUser(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> {
                                        log.warn("Current user lookup failed: email={}", email);
                                        return new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found");
                                });

                log.info("Current user fetched: userId={}, email={}, role={}", user.getId(), user.getEmail(),
                                user.getRole());
                return new CurrentUserResponse(
                                user.getId(),
                                user.getName(),
                                user.getPhone(),
                                user.getEmail(),
                                user.getRole().name());
        }

        @Transactional
        public User getOrCreateInternallyVerifiedCustomer(String phone) {
                return userRepository.findByPhone(phone)
                                .map(user -> {
                                        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.ADMIN) {
                                                log.warn("Phone checkout rejected, phone belongs to non-customer role: userId={}, phone={}, role={}",
                                                                user.getId(), phone, user.getRole());
                                                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                                                "Phone number is already used by another account type");
                                        }
                                        log.info("Phone checkout using existing customer: userId={}, phone={}",
                                                        user.getId(), phone);
                                        return user;
                                })
                                .orElseGet(() -> {
                                        String syntheticEmail = "phone-" + phone.replaceAll("[^0-9]", "")
                                                        + "@internal.foodie";
                                        User user = createUser(
                                                        "Customer " + phone,
                                                        phone,
                                                        syntheticEmail,
                                                        UUID.randomUUID().toString(),
                                                        Role.CUSTOMER);
                                        log.info("Phone checkout created customer after internal SMS verification: userId={}, phone={}",
                                                        user.getId(), phone);
                                        return user;
                                });
        }

        public AuthResponse issueTokens(User user) {
                String accessToken = jwtService.generateAccessToken(user.getEmail());

                RefreshToken refreshToken = new RefreshToken();
                refreshToken.setToken(UUID.randomUUID().toString());
                refreshToken.setUser(user);
                refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
                refreshTokenRepository.save(refreshToken);
                log.info("Auth tokens issued: userId={}, role={}, refreshTokenExpiresAt={}", user.getId(),
                                user.getRole(), refreshToken.getExpiryDate());

                return buildAuthResponse(user, accessToken, refreshToken.getToken());
        }

        private User createUser(String name, String phone, String email, String password, Role role) {
                User user = new User();
                user.setName(name);
                user.setPhone(phone);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(role);
                return userRepository.save(user);
        }

        private void ensureEmailAvailable(String email) {
                if (userRepository.existsByEmail(email)) {
                        log.warn("Signup rejected, email already registered: email={}", email);
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email is already registered");
                }
        }

        private void ensurePhoneAvailable(String phone) {
                if (userRepository.existsByPhone(phone)) {
                        log.warn("Signup rejected, phone already registered: phone={}", phone);
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Phone is already registered");
                }
        }

        private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
                return new AuthResponse(
                                accessToken,
                                refreshToken,
                                user.getId(),
                                user.getRole().name());
        }
}
