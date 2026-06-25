package com.pvc.foodie.feature.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.config.PaymentProperties;
import com.pvc.foodie.feature.order.dto.RazorpayTransferPlan;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpaySplitPaymentService {

    private static final String RAZORPAY_ACCOUNT_ID_PATTERN = "^acc_[A-Za-z0-9]{14}$";

    private final PaymentProperties paymentProperties;
    private final RestaurantRepository restaurantRepository;

    public RazorpayTransferPlan planTransfers(Order order) {
        BigDecimal commissionRate = paymentProperties.commissionRate();
        requireValidCommissionRate(commissionRate);

        String restaurantAccountId = restaurantRepository.findRazorpayLinkedAccountIdById(order.getRestaurant().getId())
                .orElse(null);
        restaurantAccountId = normalizeAccountId(restaurantAccountId);
        log.info("Restaurant Razorpay linked account lookup: orderId={}, restaurantId={}, configured={}, accountIdLength={}",
                order.getId(), order.getRestaurant().getId(), restaurantAccountId != null,
                restaurantAccountId == null ? 0 : restaurantAccountId.length());
        requireConfiguredAccountId(restaurantAccountId, "Restaurant Razorpay linked account is not configured");
        requireValidAccountId(restaurantAccountId, "Restaurant Razorpay linked account id is invalid");

        Long payableNowAmount = toCurrencySubunits(order.getTotal().subtract(order.getDeliveryCharge()));
        Long platformCommissionAmount = order.getTotal()
                .multiply(commissionRate)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        Long restaurantAmount = payableNowAmount - platformCommissionAmount;
        requirePositiveRestaurantAmount(restaurantAmount);

        String adminAccountId = normalizeAccountId(paymentProperties.adminRazorpayAccountId());
        if (isAccountConfigured(adminAccountId)) {
            requireValidAccountId(adminAccountId, "Admin Razorpay linked account id is invalid");
        }
        Long adminTransferAmount = isAccountConfigured(adminAccountId) ? platformCommissionAmount : 0L;
        log.info("Razorpay split transfer plan: orderId={}, restaurantAccountIdLength={}, adminTransferConfigured={}",
                order.getId(), restaurantAccountId.length(), adminAccountId != null);
        return new RazorpayTransferPlan(
                restaurantAmount,
                platformCommissionAmount,
                adminTransferAmount,
                restaurantAccountId,
                adminAccountId);
    }

    public BigDecimal toMajorUnits(Long amount) {
        return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
    }

    private Long toCurrencySubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private void requireValidCommissionRate(BigDecimal commissionRate) {
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Payment commission rate must be between 0 and 1");
        }
    }

    private String normalizeAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        return accountId.trim();
    }

    private void requireConfiguredAccountId(String accountId, String message) {
        if (!isAccountConfigured(accountId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    private void requireValidAccountId(String accountId, String message) {
        if (!accountId.matches(RAZORPAY_ACCOUNT_ID_PATTERN)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message + ". Expected format acc_XXXXXXXXXXXXXX");
        }
    }

    private void requirePositiveRestaurantAmount(Long restaurantAmount) {
        if (restaurantAmount <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Restaurant payout amount must be greater than zero");
        }
    }

    private boolean isAccountConfigured(String accountId) {
        return accountId != null && !accountId.isBlank();
    }
}
