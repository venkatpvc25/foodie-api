package com.pvc.foodie.feature.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.admin.dto.AdminCouponResponse;
import com.pvc.foodie.feature.audit.entity.AuditAction;
import com.pvc.foodie.feature.audit.entity.AuditEntityType;
import com.pvc.foodie.feature.audit.service.AuditLogService;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.coupon.dto.CouponRequest;
import com.pvc.foodie.feature.coupon.dto.CouponResponse;
import com.pvc.foodie.feature.coupon.entity.Coupon;
import com.pvc.foodie.feature.coupon.repository.CouponRepository;
import com.pvc.foodie.feature.coupon.service.CouponService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final AdminAccessService adminAccessService;
    private final AdminMapper adminMapper;
    private final AuditLogService auditLogService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public List<AdminCouponResponse> getCoupons(Boolean active, UUID restaurantId) {
        adminAccessService.requireAdmin();
        return couponRepository.findAll().stream()
                .filter(coupon -> active == null || coupon.isActive() == active)
                .filter(coupon -> restaurantId == null
                        || (coupon.getRestaurant() != null && coupon.getRestaurant().getId().equals(restaurantId)))
                .map(adminMapper::toCouponResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCouponResponse getCoupon(UUID id) {
        adminAccessService.requireAdmin();
        return adminMapper.toCouponResponse(requireCoupon(id));
    }

    @Transactional
    public AdminCouponResponse createCoupon(CouponRequest request) {
        User admin = adminAccessService.requireAdmin();
        CouponResponse created = couponService.createCoupon(request);
        Coupon coupon = requireCoupon(created.id());
        auditLogService.record(
                admin,
                AuditAction.COUPON_CREATED,
                AuditEntityType.COUPON,
                coupon.getId(),
                coupon.getCode(),
                "Created coupon active=" + coupon.isActive() + ", discountType=" + coupon.getDiscountType());
        return adminMapper.toCouponResponse(coupon);
    }

    @Transactional
    public AdminCouponResponse updateCoupon(UUID id, CouponRequest request) {
        User admin = adminAccessService.requireAdmin();
        CouponResponse updated = couponService.updateCoupon(id, request);
        Coupon coupon = requireCoupon(updated.id());
        auditLogService.record(
                admin,
                AuditAction.COUPON_UPDATED,
                AuditEntityType.COUPON,
                coupon.getId(),
                coupon.getCode(),
                "Updated coupon active=" + coupon.isActive() + ", usageLimit=" + coupon.getUsageLimit());
        return adminMapper.toCouponResponse(coupon);
    }

    @Transactional
    public AdminCouponResponse setCouponActive(UUID id, boolean active) {
        User admin = adminAccessService.requireAdmin();
        Coupon coupon = requireCoupon(id);
        coupon.setActive(active);
        auditLogService.record(
                admin,
                active ? AuditAction.COUPON_ACTIVATED : AuditAction.COUPON_DEACTIVATED,
                AuditEntityType.COUPON,
                coupon.getId(),
                coupon.getCode(),
                "Set active=" + active);
        return adminMapper.toCouponResponse(coupon);
    }

    @Transactional
    public void deleteCoupon(UUID id) {
        User admin = adminAccessService.requireAdmin();
        Coupon coupon = requireCoupon(id);
        auditLogService.record(
                admin,
                AuditAction.COUPON_DELETED,
                AuditEntityType.COUPON,
                coupon.getId(),
                coupon.getCode(),
                "Deleted coupon");
        couponService.deleteCoupon(id);
    }

    private Coupon requireCoupon(UUID id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Coupon not found"));
    }
}
