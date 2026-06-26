package com.pvc.foodie.feature.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.admin.dto.AdminRestaurantCreateRequest;
import com.pvc.foodie.feature.admin.dto.AdminRestaurantResponse;
import com.pvc.foodie.feature.admin.dto.AdminRestaurantUpdateRequest;
import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;
import com.pvc.foodie.feature.audit.service.AuditLogService;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.auth.repository.UserRepository;
import com.pvc.foodie.feature.restaurant.entity.Restaurant;
import com.pvc.foodie.feature.restaurant.repository.RestaurantRepository;
import com.pvc.foodie.feature.notification.event.RestaurantCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRestaurantService {

    private final AdminAccessService adminAccessService;
    private final AdminMapper adminMapper;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AdminRestaurantResponse> getRestaurants(
            String search,
            Boolean open,
            Boolean approved,
            Boolean suspended,
            Boolean missingRazorpayAccount) {
        adminAccessService.requireAdmin();
        return restaurantRepository.findAll().stream()
                .filter(restaurant -> matchesSearch(restaurant, search))
                .filter(restaurant -> open == null || restaurant.isOpen() == open)
                .filter(restaurant -> approved == null || restaurant.isApproved() == approved)
                .filter(restaurant -> suspended == null || restaurant.isSuspended() == suspended)
                .filter(restaurant -> missingRazorpayAccount == null
                        || missingRazorpayAccount == isBlank(restaurant.getRazorpayLinkedAccountId()))
                .map(adminMapper::toRestaurantResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRestaurantResponse getRestaurant(UUID id) {
        adminAccessService.requireAdmin();
        return adminMapper.toRestaurantResponse(requireRestaurant(id));
    }

    @Transactional
    public AdminRestaurantResponse createRestaurant(AdminRestaurantCreateRequest request) {
        User admin = adminAccessService.requireAdmin();
        Restaurant restaurant = new Restaurant();
        restaurant.setCreatedBy(requireRestaurantOwner(request.ownerId()));
        applyCreateRequest(restaurant, request);
        Restaurant saved = restaurantRepository.save(restaurant);
        eventPublisher.publishEvent(new RestaurantCreatedEvent(saved));
        auditLogService.record(
                admin,
                AuditAction.RESTAURANT_CREATED,
                AuditEntityType.RESTAURANT,
                saved.getId(),
                saved.getName(),
                "Created restaurant ownerId=" + saved.getCreatedBy().getId());
        log.info("Admin restaurant created: adminId={}, restaurantId={}, ownerId={}, approved={}, suspended={}",
                admin.getId(), saved.getId(), saved.getCreatedBy().getId(), saved.isApproved(), saved.isSuspended());
        return adminMapper.toRestaurantResponse(saved);
    }

    @Transactional
    public AdminRestaurantResponse updateRestaurant(UUID id, AdminRestaurantUpdateRequest request) {
        User admin = adminAccessService.requireAdmin();
        Restaurant restaurant = requireRestaurant(id);
        String oldRazorpayAccountId = restaurant.getRazorpayLinkedAccountId();
        applyUpdate(restaurant, request);
        auditLogService.record(
                admin,
                AuditAction.RESTAURANT_UPDATED,
                AuditEntityType.RESTAURANT,
                restaurant.getId(),
                restaurant.getName(),
                "Updated restaurant open=" + restaurant.isOpen() + ", approved=" + restaurant.isApproved()
                        + ", suspended=" + restaurant.isSuspended());
        if (request.razorpayLinkedAccountId() != null && !sameValue(oldRazorpayAccountId, restaurant.getRazorpayLinkedAccountId())) {
            auditLogService.record(
                    admin,
                    AuditAction.RESTAURANT_RAZORPAY_ACCOUNT_UPDATED,
                    AuditEntityType.RESTAURANT,
                    restaurant.getId(),
                    restaurant.getName(),
                    "Razorpay account changed from " + oldRazorpayAccountId + " to "
                            + restaurant.getRazorpayLinkedAccountId());
        }
        log.info("Admin restaurant updated: adminId={}, restaurantId={}, commissionRate={}, open={}",
                admin.getId(), restaurant.getId(), restaurant.getCommissionRate(), restaurant.isOpen());
        return adminMapper.toRestaurantResponse(restaurant);
    }

    @Transactional
    public AdminRestaurantResponse approveRestaurant(UUID id) {
        User admin = adminAccessService.requireAdmin();
        Restaurant restaurant = requireRestaurant(id);
        restaurant.setApproved(true);
        auditLogService.record(
                admin,
                AuditAction.RESTAURANT_APPROVED,
                AuditEntityType.RESTAURANT,
                restaurant.getId(),
                restaurant.getName(),
                "Approved restaurant");
        log.info("Admin restaurant approved: adminId={}, restaurantId={}", admin.getId(), restaurant.getId());
        return adminMapper.toRestaurantResponse(restaurant);
    }

    @Transactional
    public AdminRestaurantResponse suspendRestaurant(UUID id) {
        User admin = adminAccessService.requireAdmin();
        Restaurant restaurant = requireRestaurant(id);
        restaurant.setSuspended(true);
        restaurant.setOpen(false);
        auditLogService.record(
                admin,
                AuditAction.RESTAURANT_SUSPENDED,
                AuditEntityType.RESTAURANT,
                restaurant.getId(),
                restaurant.getName(),
                "Suspended restaurant and forced open=false");
        log.info("Admin restaurant suspended: adminId={}, restaurantId={}", admin.getId(), restaurant.getId());
        return adminMapper.toRestaurantResponse(restaurant);
    }

    @Transactional
    public AdminRestaurantResponse unsuspendRestaurant(UUID id) {
        User admin = adminAccessService.requireAdmin();
        Restaurant restaurant = requireRestaurant(id);
        restaurant.setSuspended(false);
        auditLogService.record(
                admin,
                AuditAction.RESTAURANT_UNSUSPENDED,
                AuditEntityType.RESTAURANT,
                restaurant.getId(),
                restaurant.getName(),
                "Unsuspended restaurant");
        log.info("Admin restaurant unsuspended: adminId={}, restaurantId={}", admin.getId(), restaurant.getId());
        return adminMapper.toRestaurantResponse(restaurant);
    }

    private void applyCreateRequest(Restaurant restaurant, AdminRestaurantCreateRequest request) {
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setImageUrl(request.imageUrl());
        restaurant.setPhone(request.phone());
        restaurant.setAddress(request.address());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setOpen(request.open() && !request.suspended());
        restaurant.setApproved(request.approved());
        restaurant.setSuspended(request.suspended());
        requireValidRazorpayAccountId(request.razorpayLinkedAccountId());
        restaurant.setRazorpayLinkedAccountId(blankToNull(request.razorpayLinkedAccountId()));
        restaurant.setCommissionRate(request.commissionRate());
    }

    private void applyUpdate(Restaurant restaurant, AdminRestaurantUpdateRequest request) {
        if (request.name() != null) {
            restaurant.setName(request.name());
        }
        if (request.description() != null) {
            restaurant.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            restaurant.setImageUrl(request.imageUrl());
        }
        if (request.phone() != null) {
            restaurant.setPhone(request.phone());
        }
        if (request.address() != null) {
            restaurant.setAddress(request.address());
        }
        if (request.latitude() != null) {
            restaurant.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            restaurant.setLongitude(request.longitude());
        }
        if (request.open() != null) {
            restaurant.setOpen(request.open() && !restaurant.isSuspended());
        }
        if (request.approved() != null) {
            restaurant.setApproved(request.approved());
        }
        if (request.suspended() != null) {
            restaurant.setSuspended(request.suspended());
            if (request.suspended()) {
                restaurant.setOpen(false);
            }
        }
        if (request.ownerId() != null) {
            restaurant.setCreatedBy(requireRestaurantOwner(request.ownerId()));
        }
        if (request.razorpayLinkedAccountId() != null) {
            requireValidRazorpayAccountId(request.razorpayLinkedAccountId());
            restaurant.setRazorpayLinkedAccountId(blankToNull(request.razorpayLinkedAccountId()));
        }
        if (request.commissionRate() != null) {
            restaurant.setCommissionRate(request.commissionRate());
        }
    }

    private Restaurant requireRestaurant(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant not found"));
    }

    private User requireRestaurantOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant owner not found"));
        if (owner.getRole() != Role.RESTAURANT && owner.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Restaurant owner must be restaurant or admin user");
        }
        return owner;
    }

    private boolean matchesSearch(Restaurant restaurant, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.toLowerCase();
        return contains(restaurant.getName(), normalized)
                || contains(restaurant.getPhone(), normalized)
                || contains(restaurant.getAddress(), normalized)
                || contains(restaurant.getCreatedBy() == null ? null : restaurant.getCreatedBy().getEmail(), normalized);
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

    private boolean sameValue(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private void requireValidRazorpayAccountId(String value) {
        if (value != null && !value.isBlank() && !value.startsWith("acc_")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Razorpay linked account id must start with acc_");
        }
    }
}
