package com.pvc.foodie.feature.rating.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.repository.OrderRepository;
import com.pvc.foodie.feature.rating.dto.DeliveryPartnerRatingResponse;
import com.pvc.foodie.feature.rating.dto.DeliveryTipRequest;
import com.pvc.foodie.feature.rating.dto.DeliveryTipResponse;
import com.pvc.foodie.feature.rating.dto.MenuItemRatingResponse;
import com.pvc.foodie.feature.rating.dto.RatingRequest;
import com.pvc.foodie.feature.rating.dto.RatingSummary;
import com.pvc.foodie.feature.rating.dto.RestaurantRatingResponse;
import com.pvc.foodie.feature.rating.entity.DeliveryPartnerRating;
import com.pvc.foodie.feature.rating.entity.DeliveryPartnerTip;
import com.pvc.foodie.feature.rating.entity.MenuItemRating;
import com.pvc.foodie.feature.rating.entity.RestaurantRating;
import com.pvc.foodie.feature.rating.repository.DeliveryPartnerRatingRepository;
import com.pvc.foodie.feature.rating.repository.DeliveryPartnerTipRepository;
import com.pvc.foodie.feature.rating.repository.MenuItemRatingRepository;
import com.pvc.foodie.feature.rating.repository.RestaurantRatingRepository;
import com.pvc.foodie.feature.restaurant.entity.MenuItem;
import com.pvc.foodie.feature.restaurant.repository.MenuItemRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final CurrentUserService currentUserService;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRatingRepository restaurantRatingRepository;
    private final MenuItemRatingRepository menuItemRatingRepository;
    private final DeliveryPartnerRatingRepository deliveryPartnerRatingRepository;
    private final DeliveryPartnerTipRepository deliveryPartnerTipRepository;

    @Transactional
    public RestaurantRatingResponse rateRestaurant(UUID orderId, RatingRequest request) {
        User customer = currentCustomer();
        Order order = deliveredCustomerOrder(orderId, customer);
        RestaurantRating rating = restaurantRatingRepository.findByOrderIdAndCustomerId(orderId, customer.getId())
                .orElseGet(RestaurantRating::new);
        rating.setOrder(order);
        rating.setCustomer(customer);
        rating.setRestaurant(order.getRestaurant());
        rating.setRating(request.rating());
        rating.setComment(request.comment());
        RestaurantRating saved = restaurantRatingRepository.save(rating);
        log.info("Restaurant rating saved: customerId={}, orderId={}, restaurantId={}, rating={}",
                customer.getId(), orderId, order.getRestaurant().getId(), request.rating());
        return toRestaurantRatingResponse(saved);
    }

    @Transactional
    public MenuItemRatingResponse rateMenuItem(UUID orderId, UUID menuItemId, RatingRequest request) {
        User customer = currentCustomer();
        Order order = deliveredCustomerOrder(orderId, customer);
        if (order.getItems().stream().noneMatch(item -> item.getMenuItemId().equals(menuItemId))) {
            log.warn("Menu item rating rejected because item was not part of order: customerId={}, orderId={}, menuItemId={}",
                    customer.getId(), orderId, menuItemId);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Menu item was not part of this order");
        }
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(menuItemId, order.getRestaurant().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Menu item not found"));
        MenuItemRating rating = menuItemRatingRepository
                .findByOrderIdAndMenuItemIdAndCustomerId(orderId, menuItemId, customer.getId())
                .orElseGet(MenuItemRating::new);
        rating.setOrder(order);
        rating.setCustomer(customer);
        rating.setRestaurant(order.getRestaurant());
        rating.setMenuItem(menuItem);
        rating.setRating(request.rating());
        rating.setComment(request.comment());
        MenuItemRating saved = menuItemRatingRepository.save(rating);
        log.info("Menu item rating saved: customerId={}, orderId={}, menuItemId={}, rating={}",
                customer.getId(), orderId, menuItemId, request.rating());
        return toMenuItemRatingResponse(saved);
    }

    @Transactional
    public DeliveryPartnerRatingResponse rateDeliveryPartner(UUID orderId, RatingRequest request) {
        User customer = currentCustomer();
        Order order = deliveredCustomerOrder(orderId, customer);
        if (order.getDeliveryPartner() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order has no delivery partner");
        }
        DeliveryPartnerRating rating = deliveryPartnerRatingRepository.findByOrderIdAndCustomerId(orderId, customer.getId())
                .orElseGet(DeliveryPartnerRating::new);
        rating.setOrder(order);
        rating.setCustomer(customer);
        rating.setDeliveryPartner(order.getDeliveryPartner());
        rating.setRating(request.rating());
        rating.setComment(request.comment());
        DeliveryPartnerRating saved = deliveryPartnerRatingRepository.save(rating);
        log.info("Delivery partner rating saved: customerId={}, orderId={}, deliveryPartnerId={}, rating={}",
                customer.getId(), orderId, order.getDeliveryPartner().getId(), request.rating());
        return toDeliveryPartnerRatingResponse(saved);
    }

    @Transactional
    public DeliveryTipResponse tipDeliveryPartner(UUID orderId, DeliveryTipRequest request) {
        User customer = currentCustomer();
        Order order = deliveredCustomerOrder(orderId, customer);
        if (order.getDeliveryPartner() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order has no delivery partner");
        }
        DeliveryPartnerTip tip = deliveryPartnerTipRepository.findByOrderIdAndCustomerId(orderId, customer.getId())
                .orElseGet(DeliveryPartnerTip::new);
        tip.setOrder(order);
        tip.setCustomer(customer);
        tip.setDeliveryPartner(order.getDeliveryPartner());
        tip.setAmount(request.amount());
        tip.setNote(request.note());
        DeliveryPartnerTip saved = deliveryPartnerTipRepository.save(tip);
        log.info("Delivery partner tip saved: customerId={}, orderId={}, deliveryPartnerId={}, amount={}",
                customer.getId(), orderId, order.getDeliveryPartner().getId(), request.amount());
        return toDeliveryTipResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RestaurantRatingResponse> getRestaurantRatings(UUID restaurantId) {
        return restaurantRatingRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(this::toRestaurantRatingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemRatingResponse> getMenuItemRatings(UUID menuItemId) {
        return menuItemRatingRepository.findByMenuItemIdOrderByCreatedAtDesc(menuItemId).stream()
                .map(this::toMenuItemRatingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryPartnerRatingResponse> getDeliveryPartnerRatings(UUID deliveryPartnerId) {
        return deliveryPartnerRatingRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(deliveryPartnerId).stream()
                .map(this::toDeliveryPartnerRatingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryTipResponse> getDeliveryPartnerTips(UUID deliveryPartnerId) {
        User user = currentUserService.getCurrentUser();
        boolean ownDeliveryPartnerTips = user.getRole() == Role.DELIVERY_PARTNER && user.getId().equals(deliveryPartnerId);
        if (user.getRole() != Role.ADMIN && !ownDeliveryPartnerTips) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Delivery tip access required");
        }
        return deliveryPartnerTipRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(deliveryPartnerId).stream()
                .map(this::toDeliveryTipResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RatingSummary getRestaurantRatingSummary(UUID restaurantId) {
        return toSummary(restaurantRatingRepository.getSummary(restaurantId));
    }

    @Transactional(readOnly = true)
    public RatingSummary getMenuItemRatingSummary(UUID menuItemId) {
        return toSummary(menuItemRatingRepository.getSummary(menuItemId));
    }

    @Transactional(readOnly = true)
    public RatingSummary getDeliveryPartnerRatingSummary(UUID deliveryPartnerId) {
        return toSummary(deliveryPartnerRatingRepository.getSummary(deliveryPartnerId));
    }

    @Transactional(readOnly = true)
    public BigDecimal getDeliveryPartnerTipTotal(UUID deliveryPartnerId) {
        BigDecimal total = deliveryPartnerTipRepository.sumAmountByDeliveryPartnerId(deliveryPartnerId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private User currentCustomer() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Customer access required");
        }
        return user;
    }

    private Order deliveredCustomerOrder(UUID orderId, User customer) {
        Order order = orderRepository.findByIdAndUserId(orderId, customer.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order must be delivered before rating or tipping");
        }
        return order;
    }

    private RatingSummary toSummary(Object[] values) {
        if (values == null || values.length < 2) {
            return new RatingSummary(0, 0);
        }
        double average = values[0] instanceof Number number ? number.doubleValue() : 0;
        long count = values[1] instanceof Number number ? number.longValue() : 0;
        return new RatingSummary(average, count);
    }

    private RestaurantRatingResponse toRestaurantRatingResponse(RestaurantRating rating) {
        return new RestaurantRatingResponse(
                rating.getId(),
                rating.getOrder().getId(),
                rating.getCustomer().getId(),
                rating.getCustomer().getName(),
                rating.getRestaurant().getId(),
                rating.getRating(),
                rating.getComment(),
                rating.getCreatedAt());
    }

    private MenuItemRatingResponse toMenuItemRatingResponse(MenuItemRating rating) {
        return new MenuItemRatingResponse(
                rating.getId(),
                rating.getOrder().getId(),
                rating.getCustomer().getId(),
                rating.getCustomer().getName(),
                rating.getRestaurant().getId(),
                rating.getMenuItem().getId(),
                rating.getMenuItem().getName(),
                rating.getRating(),
                rating.getComment(),
                rating.getCreatedAt());
    }

    private DeliveryPartnerRatingResponse toDeliveryPartnerRatingResponse(DeliveryPartnerRating rating) {
        return new DeliveryPartnerRatingResponse(
                rating.getId(),
                rating.getOrder().getId(),
                rating.getCustomer().getId(),
                rating.getCustomer().getName(),
                rating.getDeliveryPartner().getId(),
                rating.getDeliveryPartner().getName(),
                rating.getRating(),
                rating.getComment(),
                rating.getCreatedAt());
    }

    private DeliveryTipResponse toDeliveryTipResponse(DeliveryPartnerTip tip) {
        return new DeliveryTipResponse(
                tip.getId(),
                tip.getOrder().getId(),
                tip.getCustomer().getId(),
                tip.getCustomer().getName(),
                tip.getDeliveryPartner().getId(),
                tip.getDeliveryPartner().getName(),
                tip.getAmount(),
                tip.getNote(),
                tip.getCreatedAt());
    }
}
