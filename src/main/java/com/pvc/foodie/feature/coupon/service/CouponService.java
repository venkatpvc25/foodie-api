package com.pvc.foodie.feature.coupon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.coupon.dto.AppliedCoupon;
import com.pvc.foodie.feature.coupon.dto.CouponPreviewRequest;
import com.pvc.foodie.feature.coupon.dto.CouponPreviewResponse;
import com.pvc.foodie.feature.coupon.dto.CouponRequest;
import com.pvc.foodie.feature.coupon.dto.CouponResponse;
import com.pvc.foodie.feature.coupon.entity.Coupon;
import com.pvc.foodie.feature.coupon.entity.DiscountType;
import com.pvc.foodie.feature.coupon.repository.CouponRepository;
import com.pvc.foodie.feature.restaurant.entity.Restaurant;
import com.pvc.foodie.feature.restaurant.repository.RestaurantRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CouponRepository couponRepository;
    private final RestaurantRepository restaurantRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(UUID restaurantId) {
        User user = currentUserService.getCurrentUser();
        List<Coupon> coupons = switch (user.getRole()) {
            case ADMIN -> restaurantId == null
                    ? couponRepository.findAll()
                    : couponRepository.findByRestaurantIdOrRestaurantIsNullOrderByCodeAsc(restaurantId);
            case RESTAURANT -> couponRepository.findByCreatedByIdOrderByCodeAsc(user.getId());
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED, "Coupon management access required");
        };
        return coupons.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        User user = currentUserService.getCurrentUser();
        requireCouponManager(user);
        String code = normalizeCode(request.code());
        ensureCodeAvailable(code);
        validateCouponWindow(request.validFrom(), request.validTo());

        Coupon coupon = new Coupon();
        coupon.setCreatedBy(user);
        applyRequest(coupon, request, user, code);
        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created: couponId={}, code={}, createdBy={}, restaurantId={}",
                saved.getId(), saved.getCode(), user.getId(), restaurantId(saved));
        return toResponse(saved);
    }

    @Transactional
    public CouponResponse updateCoupon(UUID id, CouponRequest request) {
        User user = currentUserService.getCurrentUser();
        requireCouponManager(user);
        Coupon coupon = getManageableCoupon(id, user);
        String code = normalizeCode(request.code());
        if (!coupon.getCode().equalsIgnoreCase(code)) {
            ensureCodeAvailable(code);
        }
        validateCouponWindow(request.validFrom(), request.validTo());
        applyRequest(coupon, request, user, code);
        log.info("Coupon updated: couponId={}, code={}, updatedBy={}, restaurantId={}",
                coupon.getId(), coupon.getCode(), user.getId(), restaurantId(coupon));
        return toResponse(coupon);
    }

    @Transactional
    public void deleteCoupon(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireCouponManager(user);
        Coupon coupon = getManageableCoupon(id, user);
        couponRepository.delete(coupon);
        log.info("Coupon deleted: couponId={}, code={}, deletedBy={}", id, coupon.getCode(), user.getId());
    }

    @Transactional(readOnly = true)
    public CouponPreviewResponse previewCoupon(CouponPreviewRequest request) {
        AppliedCoupon applied = validateAndCalculate(request.code(), request.restaurantId(), request.subtotal());
        return new CouponPreviewResponse(
                applied.coupon().getId(),
                applied.code(),
                request.subtotal(),
                applied.discountAmount(),
                request.subtotal().subtract(applied.discountAmount()));
    }

    @Transactional(readOnly = true)
    public AppliedCoupon validateAndCalculate(String code, UUID restaurantId, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return AppliedCoupon.none();
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Coupon not found"));
        validateCouponForOrder(coupon, restaurantId, subtotal);
        return new AppliedCoupon(coupon, coupon.getCode(), calculateDiscount(coupon, subtotal));
    }

    @Transactional
    public void markCouponUsed(Coupon coupon) {
        if (coupon == null) {
            return;
        }
        coupon.setUsedCount(coupon.getUsedCount() + 1);
    }

    private void applyRequest(Coupon coupon, CouponRequest request, User user, String code) {
        coupon.setCode(code);
        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setMinOrderAmount(request.minOrderAmount() == null ? BigDecimal.ZERO : request.minOrderAmount());
        coupon.setRestaurant(resolveRestaurant(request.restaurantId(), user));
        coupon.setActive(request.active());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidTo(request.validTo());
        coupon.setUsageLimit(request.usageLimit());
        requireValidDiscount(coupon);
    }

    private Restaurant resolveRestaurant(UUID restaurantId, User user) {
        if (user.getRole() == Role.RESTAURANT && restaurantId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Restaurant coupon must include restaurantId");
        }
        if (restaurantId == null) {
            return null;
        }
        return user.getRole() == Role.ADMIN
                ? restaurantRepository.findById(restaurantId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant not found"))
                : restaurantRepository.findByIdAndCreatedById(restaurantId, user.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Restaurant not found"));
    }

    private void validateCouponForOrder(Coupon coupon, UUID restaurantId, BigDecimal subtotal) {
        requireActiveCoupon(coupon);
        requireCouponInDateWindow(coupon);
        requireCouponUsageAvailable(coupon);
        requireCouponRestaurantMatch(coupon, restaurantId);
        requireMinimumOrderAmount(coupon, subtotal);
    }

    private void requireActiveCoupon(Coupon coupon) {
        if (!coupon.isActive()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coupon is not active");
        }
    }

    private void requireCouponInDateWindow(Coupon coupon) {
        Instant now = Instant.now();
        if (coupon.getValidFrom() != null && coupon.getValidFrom().isAfter(now)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coupon is not valid yet");
        }
        if (coupon.getValidTo() != null && coupon.getValidTo().isBefore(now)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coupon has expired");
        }
    }

    private void requireCouponUsageAvailable(Coupon coupon) {
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coupon usage limit reached");
        }
    }

    private void requireCouponRestaurantMatch(Coupon coupon, UUID restaurantId) {
        if (coupon.getRestaurant() != null && !coupon.getRestaurant().getId().equals(restaurantId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coupon is not valid for this restaurant");
        }
    }

    private void requireMinimumOrderAmount(Coupon coupon, BigDecimal subtotal) {
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order subtotal is below coupon minimum");
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(coupon.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : coupon.getDiscountValue();
        if (coupon.getMaxDiscountAmount() != null) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private Coupon getManageableCoupon(UUID id, User user) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Coupon not found"));
        if (user.getRole() == Role.RESTAURANT && !user.getId().equals(coupon.getCreatedBy().getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Coupon management access required");
        }
        return coupon;
    }

    private void requireCouponManager(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.RESTAURANT) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Coupon management access required");
        }
    }

    private void requireValidDiscount(Coupon coupon) {
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE && coupon.getDiscountValue().compareTo(HUNDRED) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Percentage discount cannot exceed 100");
        }
    }

    private void validateCouponWindow(Instant validFrom, Instant validTo) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coupon validFrom must be before validTo");
        }
    }

    private void ensureCodeAvailable(String code) {
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coupon code already exists");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinOrderAmount(),
                restaurantId(coupon),
                coupon.getRestaurant() == null ? null : coupon.getRestaurant().getName(),
                coupon.isActive(),
                coupon.getValidFrom(),
                coupon.getValidTo(),
                coupon.getUsageLimit(),
                coupon.getUsedCount());
    }

    private UUID restaurantId(Coupon coupon) {
        return coupon.getRestaurant() == null ? null : coupon.getRestaurant().getId();
    }
}
