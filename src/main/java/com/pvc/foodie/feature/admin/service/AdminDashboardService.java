package com.pvc.foodie.feature.admin.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.feature.admin.dto.AdminDashboardResponse;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.repository.UserRepository;
import com.pvc.foodie.feature.coupon.repository.CouponRepository;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.PaymentStatus;
import com.pvc.foodie.feature.order.repository.OrderRepository;
import com.pvc.foodie.feature.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminAccessService adminAccessService;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        adminAccessService.requireAdmin();
        var orders = orderRepository.findAll();
        BigDecimal grossOrderValue = orders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal platformCommissionTotal = orders.stream()
                .map(Order::getPlatformCommissionAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal restaurantPayoutTotal = orders.stream()
                .map(Order::getRestaurantPayoutAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deliveryPayoutTotal = orders.stream()
                .map(Order::getDeliveryPartnerPayoutAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidOrderCount = orders.stream()
                .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                .count();
        long activeCouponCount = couponRepository.findAll().stream()
                .filter(coupon -> coupon.isActive())
                .count();

        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.findByRole(Role.CUSTOMER).size(),
                userRepository.findByRole(Role.RESTAURANT).size(),
                userRepository.findByRole(Role.DELIVERY_PARTNER).size(),
                restaurantRepository.count(),
                orders.size(),
                paidOrderCount,
                grossOrderValue,
                platformCommissionTotal,
                restaurantPayoutTotal,
                deliveryPayoutTotal,
                activeCouponCount);
    }
}
