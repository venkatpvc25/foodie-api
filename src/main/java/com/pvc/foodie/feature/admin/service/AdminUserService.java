package com.pvc.foodie.feature.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.admin.dto.AdminUserCreateRequest;
import com.pvc.foodie.feature.admin.dto.AdminUserResponse;
import com.pvc.foodie.feature.admin.dto.AdminUserUpdateRequest;
import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;
import com.pvc.foodie.feature.audit.service.AuditLogService;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.auth.repository.RefreshTokenRepository;
import com.pvc.foodie.feature.auth.repository.UserRepository;
import com.pvc.foodie.feature.notification.event.UserSignedUpEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminAccessService adminAccessService;
    private final AdminMapper adminMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers(
            Role role,
            Boolean blocked,
            Boolean missingRazorpayAccount,
            String search) {
        adminAccessService.requireAdmin();
        return userRepository.findAll().stream()
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> blocked == null || user.isBlocked() == blocked)
                .filter(user -> missingRazorpayAccount == null
                        || missingRazorpayAccount == isBlank(user.getRazorpayLinkedAccountId()))
                .filter(user -> matchesSearch(user, search))
                .map(adminMapper::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID id) {
        adminAccessService.requireAdmin();
        return adminMapper.toUserResponse(requireUser(id));
    }

    @Transactional
    public AdminUserResponse createRestaurantOwner(AdminUserCreateRequest request) {
        return createManagedUser(request, Role.RESTAURANT);
    }

    @Transactional
    public AdminUserResponse createDeliveryPartner(AdminUserCreateRequest request) {
        return createManagedUser(request, Role.DELIVERY_PARTNER);
    }

    @Transactional
    public AdminUserResponse updateUser(UUID id, AdminUserUpdateRequest request) {
        User admin = adminAccessService.requireAdmin();
        User user = requireUser(id);
        boolean oldBlocked = user.isBlocked();
        applyUpdate(user, request, admin);
        auditLogService.record(
                admin,
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                user.getId(),
                user.getEmail(),
                "Updated user role=" + user.getRole() + ", blocked=" + user.isBlocked());
        if (oldBlocked != user.isBlocked()) {
            auditLogService.record(
                    admin,
                    user.isBlocked() ? AuditAction.USER_BLOCKED : AuditAction.USER_UNBLOCKED,
                    AuditEntityType.USER,
                    user.getId(),
                    user.getEmail(),
                    "Set blocked=" + user.isBlocked() + " through user update");
        }
        log.info("Admin user updated: adminId={}, userId={}, role={}, blocked={}",
                admin.getId(), user.getId(), user.getRole(), user.isBlocked());
        return adminMapper.toUserResponse(user);
    }

    @Transactional
    public AdminUserResponse blockUser(UUID id) {
        User admin = adminAccessService.requireAdmin();
        User user = requireUser(id);
        applyBlockedUpdate(user, true, admin);
        auditLogService.record(
                admin,
                AuditAction.USER_BLOCKED,
                AuditEntityType.USER,
                user.getId(),
                user.getEmail(),
                "Blocked user role=" + user.getRole());
        log.info("Admin user blocked: adminId={}, userId={}, role={}", admin.getId(), user.getId(), user.getRole());
        return adminMapper.toUserResponse(user);
    }

    @Transactional
    public AdminUserResponse unblockUser(UUID id) {
        User admin = adminAccessService.requireAdmin();
        User user = requireUser(id);
        applyBlockedUpdate(user, false, admin);
        auditLogService.record(
                admin,
                AuditAction.USER_UNBLOCKED,
                AuditEntityType.USER,
                user.getId(),
                user.getEmail(),
                "Unblocked user role=" + user.getRole());
        log.info("Admin user unblocked: adminId={}, userId={}, role={}", admin.getId(), user.getId(), user.getRole());
        return adminMapper.toUserResponse(user);
    }

    private AdminUserResponse createManagedUser(AdminUserCreateRequest request, Role role) {
        User admin = adminAccessService.requireAdmin();
        requireEmailAvailable(request.email());
        requirePhoneAvailable(request.phone());

        User user = new User();
        user.setName(request.name());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        requireValidRazorpayAccountId(request.razorpayLinkedAccountId());
        user.setRazorpayLinkedAccountId(blankToNull(request.razorpayLinkedAccountId()));

        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserSignedUpEvent(saved));
        auditLogService.record(
                admin,
                role == Role.DELIVERY_PARTNER
                        ? AuditAction.DELIVERY_PARTNER_CREATED
                        : AuditAction.RESTAURANT_OWNER_CREATED,
                AuditEntityType.USER,
                saved.getId(),
                saved.getEmail(),
                "Created managed user role=" + saved.getRole());
        log.info("Admin managed user created: adminId={}, userId={}, role={}",
                admin.getId(), saved.getId(), saved.getRole());
        return adminMapper.toUserResponse(saved);
    }

    private void applyUpdate(User user, AdminUserUpdateRequest request, User admin) {
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.email() != null) {
            requireEmailAvailable(user, request.email());
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            requirePhoneAvailable(user, request.phone());
            user.setPhone(request.phone());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.blocked() != null) {
            applyBlockedUpdate(user, request.blocked(), admin);
        }
        if (request.razorpayLinkedAccountId() != null) {
            requireValidRazorpayAccountId(request.razorpayLinkedAccountId());
            user.setRazorpayLinkedAccountId(blankToNull(request.razorpayLinkedAccountId()));
        }
    }

    private void applyBlockedUpdate(User user, boolean blocked, User admin) {
        if (user.getId().equals(admin.getId()) && blocked) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Admin cannot block their own account");
        }
        if (user.isBlocked() == blocked) {
            return;
        }
        user.setBlocked(blocked);
        if (blocked) {
            revokeRefreshTokens(user);
        }
    }

    private void revokeRefreshTokens(User user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(token -> token.setRevoked(true));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private void requireEmailAvailable(User user, String email) {
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email is already registered");
                });
    }

    private void requireEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email is already registered");
        }
    }

    private void requirePhoneAvailable(User user, String phone) {
        userRepository.findByPhone(phone)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Phone is already registered");
                });
    }

    private void requirePhoneAvailable(String phone) {
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Phone is already registered");
        }
    }

    private boolean matchesSearch(User user, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.toLowerCase();
        return contains(user.getName(), normalized)
                || contains(user.getEmail(), normalized)
                || contains(user.getPhone(), normalized);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireValidRazorpayAccountId(String value) {
        if (value != null && !value.isBlank() && !value.startsWith("acc_")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay linked account id must start with acc_");
        }
    }
}
