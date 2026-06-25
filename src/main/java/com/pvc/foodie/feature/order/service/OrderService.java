package com.pvc.foodie.feature.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.address.dto.AddressRequest;
import com.pvc.foodie.feature.address.entity.Address;
import com.pvc.foodie.feature.address.repository.AddressRepository;
import com.pvc.foodie.feature.auth.dto.AuthResponse;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.cart.entity.Cart;
import com.pvc.foodie.feature.cart.repository.CartRepository;
import com.pvc.foodie.feature.auth.service.AuthService;
import com.pvc.foodie.feature.coupon.dto.AppliedCoupon;
import com.pvc.foodie.feature.coupon.service.CouponService;
import com.pvc.foodie.feature.notification.event.DeliveryOrderClaimedEvent;
import com.pvc.foodie.feature.notification.event.OrderPlacedEvent;
import com.pvc.foodie.feature.notification.event.OrderStatusChangedEvent;
import com.pvc.foodie.feature.order.dto.DeliveryDashboardResponse;
import com.pvc.foodie.feature.order.dto.GuestCheckoutRequest;
import com.pvc.foodie.feature.order.dto.GuestCheckoutResponse;
import com.pvc.foodie.feature.order.dto.OrderResponse;
import com.pvc.foodie.feature.order.dto.PlaceOrderRequest;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderItem;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentMethod;
import com.pvc.foodie.feature.order.entity.PaymentStatus;
import com.pvc.foodie.feature.order.repository.OrderRepository;
import com.pvc.foodie.feature.rating.service.RatingService;
import com.pvc.foodie.feature.restaurant.entity.MenuItem;
import com.pvc.foodie.feature.restaurant.entity.Restaurant;
import com.pvc.foodie.feature.restaurant.repository.MenuItemRepository;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(40);
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.05);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final MenuItemRepository menuItemRepository;
    private final CurrentUserService currentUserService;
    private final AuthService authService;
    private final RazorpayPaymentService razorpayPaymentService;
    private final OrderResponseMapper orderResponseMapper;
    private final DeliveryPayoutService deliveryPayoutService;
    private final RatingService ratingService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;
    private final GuestCheckoutOtpService guestCheckoutOtpService;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.CUSTOMER, "Customer access required");
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Cart is empty"));
        requireOrderableCart(user, cart);

        Address address = addressRepository.findByIdAndUserId(request.addressId(), user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Address not found"));

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PricingSummary pricing = priceOrder(request.couponCode(), cart.getRestaurant().getId(), subtotal);

        Order order = new Order();
        order.setOrderNumber("ORD-" + Instant.now().toEpochMilli());
        order.setUser(user);
        order.setRestaurant(cart.getRestaurant());
        order.setAddress(address);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(subtotal);
        order.setDeliveryCharge(DELIVERY_CHARGE);
        applyPricing(order, pricing);
        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setNotes(request.notes());

        cart.getItems().forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItemId(cartItem.getMenuItem().getId());
            orderItem.setName(cartItem.getMenuItem().getName());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);
        });

        Order saved = orderRepository.save(order);
        couponService.markCouponUsed(pricing.appliedCoupon().coupon());
        razorpayPaymentService.createOnlinePaymentOrderIfRequired(saved);
        cart.getItems().clear();
        cart.setRestaurant(null);
        log.info("Order placed: userId={}, orderId={}, orderNumber={}, restaurantId={}, itemCount={}, subtotal={}, discount={}, tax={}, deliveryCharge={}, total={}",
                user.getId(), saved.getId(), saved.getOrderNumber(), saved.getRestaurant().getId(),
                saved.getItems().size(), saved.getSubtotal(), saved.getDiscountAmount(), saved.getTax(),
                saved.getDeliveryCharge(), saved.getTotal());
        eventPublisher.publishEvent(new OrderPlacedEvent(saved));
        return orderResponseMapper.toResponse(saved);
    }

    @Transactional
    public GuestCheckoutResponse placeGuestOrder(GuestCheckoutRequest request) {
        log.info("Guest checkout started: phone={}, itemCount={}, paymentMethod={}",
                request.phone(), request.items().size(), PaymentMethod.ONLINE);
        guestCheckoutOtpService.verify(request.phone(), request.verificationCode());

        User user = authService.getOrCreateInternallyVerifiedCustomer(request.phone());
        Address address = saveCheckoutAddress(user, request.address());

        Order order = new Order();
        order.setOrderNumber("ORD-" + Instant.now().toEpochMilli());
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setNotes(request.notes());

        Restaurant restaurant = null;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (var cartItem : request.items()) {
            MenuItem menuItem = menuItemRepository.findByIdAndAvailableTrue(cartItem.menuItemId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Menu item not found"));

            requireRestaurantOpenForGuestCheckout(request.phone(), menuItem);

            if (restaurant == null) {
                restaurant = menuItem.getRestaurant();
                order.setRestaurant(restaurant);
            } else {
                requireSameRestaurantForGuestCheckout(request.phone(), restaurant, menuItem);
            }

            BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(cartItem.quantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItemId(menuItem.getId());
            orderItem.setName(menuItem.getName());
            orderItem.setPrice(menuItem.getPrice());
            orderItem.setQuantity(cartItem.quantity());
            order.getItems().add(orderItem);
        }

        PricingSummary pricing = priceOrder(request.couponCode(), restaurant.getId(), subtotal);
        order.setSubtotal(subtotal);
        order.setDeliveryCharge(DELIVERY_CHARGE);
        applyPricing(order, pricing);

        Order saved = orderRepository.save(order);
        couponService.markCouponUsed(pricing.appliedCoupon().coupon());
        razorpayPaymentService.createOnlinePaymentOrderIfRequired(saved);
        AuthResponse auth = authService.issueTokens(user);
        log.info("Guest order placed: phone={}, userId={}, orderId={}, orderNumber={}, restaurantId={}, itemCount={}, total={}",
                request.phone(), user.getId(), saved.getId(), saved.getOrderNumber(), saved.getRestaurant().getId(),
                saved.getItems().size(), saved.getTotal());
        eventPublisher.publishEvent(new OrderPlacedEvent(saved));
        return new GuestCheckoutResponse(orderResponseMapper.toResponse(saved), auth, true);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.CUSTOMER, "Customer access required");
        List<OrderResponse> orders = orderRepository.findByUserIdOrderByIdDesc(user.getId()).stream()
                .map(orderResponseMapper::toResponse)
                .toList();
        log.info("Customer orders fetched: userId={}, count={}", user.getId(), orders.size());
        return orders;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.CUSTOMER, "Customer access required");
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        log.info("Customer order fetched: userId={}, orderId={}, status={}", user.getId(), id, order.getStatus());
        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.CUSTOMER, "Customer access required");
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        requireCustomerCancellableOrder(user, id, order);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order cancelled by customer: userId={}, orderId={}", user.getId(), id);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order, oldStatus, OrderStatus.CANCELLED));
        return orderResponseMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders() {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.RESTAURANT, "Restaurant access required");
        List<OrderResponse> orders = orderRepository.findByRestaurantCreatedByIdOrderByIdDesc(user.getId()).stream()
                .map(orderResponseMapper::toResponse)
                .toList();
        log.info("Restaurant orders fetched: restaurantUserId={}, count={}", user.getId(), orders.size());
        return orders;
    }

    @Transactional
    public OrderResponse updateRestaurantOrderStatus(UUID id, OrderStatus status) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.RESTAURANT, "Restaurant access required");
        Order order = orderRepository.findByIdAndRestaurantCreatedById(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        requireValidRestaurantStatusUpdate(user, id, order, status);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        log.info("Restaurant order status updated: restaurantUserId={}, orderId={}, oldStatus={}, newStatus={}",
                user.getId(), id, oldStatus, status);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order, oldStatus, status));
        return orderResponseMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAvailableDeliveryOrders() {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.DELIVERY_PARTNER, "Delivery partner access required");
        List<OrderResponse> orders = orderRepository
                .findByStatusAndPaymentStatusAndDeliveryPartnerIsNullOrderByIdDesc(
                        OrderStatus.READY,
                        PaymentStatus.PAID)
                .stream()
                .map(orderResponseMapper::toResponse)
                .toList();
        log.info("Available delivery orders fetched: deliveryPartnerId={}, count={}", user.getId(), orders.size());
        return orders;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyDeliveryOrders() {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.DELIVERY_PARTNER, "Delivery partner access required");
        List<OrderResponse> orders = orderRepository.findByDeliveryPartnerIdOrderByIdDesc(user.getId()).stream()
                .map(orderResponseMapper::toResponse)
                .toList();
        log.info("Delivery partner orders fetched: deliveryPartnerId={}, count={}", user.getId(), orders.size());
        return orders;
    }

    @Transactional(readOnly = true)
    public DeliveryDashboardResponse getDeliveryDashboard() {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.DELIVERY_PARTNER, "Delivery partner access required");
        List<Order> orders = orderRepository.findByDeliveryPartnerIdOrderByIdDesc(user.getId());
        List<OrderResponse> activeOrders = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED)
                .map(orderResponseMapper::toResponse)
                .toList();
        List<OrderResponse> deliveredOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(orderResponseMapper::toResponse)
                .toList();
        BigDecimal totalEarnings = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getDeliveryCharge)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTips = ratingService.getDeliveryPartnerTipTotal(user.getId());
        BigDecimal totalEarningsWithTips = totalEarnings.add(totalTips);
        log.info("Delivery dashboard fetched: deliveryPartnerId={}, activeOrderCount={}, deliveredOrderCount={}, totalEarnings={}, totalTips={}",
                user.getId(), activeOrders.size(), deliveredOrders.size(), totalEarnings, totalTips);
        return new DeliveryDashboardResponse(
                activeOrders.size(),
                deliveredOrders.size(),
                totalEarnings,
                totalTips,
                totalEarningsWithTips,
                activeOrders,
                deliveredOrders);
    }

    @Transactional
    public OrderResponse claimDeliveryOrder(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.DELIVERY_PARTNER, "Delivery partner access required");
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        requireClaimableDeliveryOrder(user, id, order);
        order.setDeliveryPartner(user);
        log.info("Delivery order claimed: deliveryPartnerId={}, orderId={}", user.getId(), id);
        eventPublisher.publishEvent(new DeliveryOrderClaimedEvent(order));
        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse pickupDeliveryOrder(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.DELIVERY_PARTNER, "Delivery partner access required");
        Order order = orderRepository.findByIdAndDeliveryPartnerId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        requirePickupReadyOrder(order);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.PICKED_UP);
        log.info("Delivery order picked up: deliveryPartnerId={}, orderId={}", user.getId(), id);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order, oldStatus, OrderStatus.PICKED_UP));
        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse deliverDeliveryOrder(UUID id) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.DELIVERY_PARTNER, "Delivery partner access required");
        Order order = orderRepository.findByIdAndDeliveryPartnerId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        requireDeliverableOrder(order);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);
        deliveryPayoutService.transferDeliveryChargeIfDelivered(order);
        log.info("Delivery order delivered: deliveryPartnerId={}, orderId={}", user.getId(), id);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order, oldStatus, OrderStatus.DELIVERED));
        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse updateDeliveryOrderStatus(UUID id, OrderStatus status) {
        User user = currentUserService.getCurrentUser();
        requireRole(user, Role.DELIVERY_PARTNER, "Delivery partner access required");
        Order order = orderRepository.findByIdAndDeliveryPartnerId(id, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Order not found"));
        requireValidDeliveryStatusUpdate(user, id, order, status);
        OrderStatus oldStatus = order.getStatus();
        requireDeliveryStatusTransition(order, status);
        order.setStatus(status);
        deliveryPayoutService.transferDeliveryChargeIfDelivered(order);
        log.info("Delivery order status updated: deliveryPartnerId={}, orderId={}, oldStatus={}, newStatus={}, paymentStatus={}",
                user.getId(), id, oldStatus, status, order.getPaymentStatus());
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order, oldStatus, status));
        return orderResponseMapper.toResponse(order);
    }

    private Address saveCheckoutAddress(User user, AddressRequest request) {
        Address address = new Address();
        address.setUser(user);
        address.setTitle(request.title());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        address.setDefaultAddress(request.defaultAddress());
        Address saved = addressRepository.save(address);
        log.info("Guest checkout address saved: userId={}, addressId={}, city={}",
                user.getId(), saved.getId(), saved.getCity());
        return saved;
    }

    private PricingSummary priceOrder(String couponCode, UUID restaurantId, BigDecimal subtotal) {
        AppliedCoupon appliedCoupon = couponService.validateAndCalculate(couponCode, restaurantId, subtotal);
        BigDecimal discountedSubtotal = subtotal.subtract(appliedCoupon.discountAmount());
        BigDecimal tax = discountedSubtotal.multiply(TAX_RATE);
        BigDecimal total = discountedSubtotal.add(DELIVERY_CHARGE).add(tax);
        return new PricingSummary(appliedCoupon, tax, total);
    }

    private void applyPricing(Order order, PricingSummary pricing) {
        order.setCoupon(pricing.appliedCoupon().coupon());
        order.setCouponCode(pricing.appliedCoupon().code());
        order.setDiscountAmount(pricing.appliedCoupon().discountAmount());
        order.setTax(pricing.tax());
        order.setTotal(pricing.total());
    }

    private void requireOrderableCart(User user, Cart cart) {
        if (cart.getItems().isEmpty() || cart.getRestaurant() == null) {
            log.warn("Order placement rejected because cart is empty: userId={}, cartId={}",
                    user.getId(), cart.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cart is empty");
        }
    }

    private void requireRestaurantOpenForGuestCheckout(String phone, MenuItem menuItem) {
        if (!menuItem.getRestaurant().isOpen()) {
            log.warn("Guest checkout rejected, restaurant is closed: phone={}, restaurantId={}",
                    phone, menuItem.getRestaurant().getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Restaurant is closed");
        }
    }

    private void requireSameRestaurantForGuestCheckout(String phone, Restaurant restaurant, MenuItem menuItem) {
        if (!restaurant.getId().equals(menuItem.getRestaurant().getId())) {
            log.warn("Guest checkout rejected, items from multiple restaurants: phone={}, firstRestaurantId={}, otherRestaurantId={}",
                    phone, restaurant.getId(), menuItem.getRestaurant().getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "All items in an order must be from the same restaurant");
        }
    }

    private void requireCustomerCancellableOrder(User user, UUID orderId, Order order) {
        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.ACCEPTED) {
            log.warn("Order cancellation rejected: userId={}, orderId={}, currentStatus={}",
                    user.getId(), orderId, order.getStatus());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order cannot be cancelled");
        }
    }

    private void requireValidRestaurantStatusUpdate(User user, UUID orderId, Order order, OrderStatus status) {
        requireRestaurantTargetStatus(user, orderId, status);
        requireRestaurantOrderOpenForUpdate(user, orderId, order, status);
        requireRestaurantCancellationAllowed(user, orderId, order, status);
    }

    private void requireRestaurantTargetStatus(User user, UUID orderId, OrderStatus status) {
        if (!List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.CANCELLED)
                .contains(status)) {
            log.warn("Restaurant status update rejected due to invalid target: restaurantUserId={}, orderId={}, requestedStatus={}",
                    user.getId(), orderId, status);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid restaurant order status");
        }
    }

    private void requireRestaurantOrderOpenForUpdate(User user, UUID orderId, Order order, OrderStatus status) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            log.warn("Restaurant status update rejected because order is closed: restaurantUserId={}, orderId={}, currentStatus={}, requestedStatus={}",
                    user.getId(), orderId, order.getStatus(), status);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order status cannot be changed");
        }
    }

    private void requireRestaurantCancellationAllowed(User user, UUID orderId, Order order, OrderStatus status) {
        if (status == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.PLACED
                && order.getStatus() != OrderStatus.ACCEPTED) {
            log.warn("Restaurant cancellation rejected: restaurantUserId={}, orderId={}, currentStatus={}",
                    user.getId(), orderId, order.getStatus());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order cannot be cancelled by restaurant");
        }
    }

    private void requireClaimableDeliveryOrder(User user, UUID orderId, Order order) {
        if (order.getStatus() != OrderStatus.READY || order.getDeliveryPartner() != null) {
            UUID assignedDeliveryPartnerId = order.getDeliveryPartner() == null ? null : order.getDeliveryPartner().getId();
            log.warn("Delivery claim rejected: deliveryPartnerId={}, orderId={}, currentStatus={}, assignedDeliveryPartnerId={}",
                    user.getId(), orderId, order.getStatus(), assignedDeliveryPartnerId);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order is not available for delivery");
        }
        requirePaidOrder(order, "Online payment must be completed before delivery");
    }

    private void requirePickupReadyOrder(Order order) {
        if (order.getStatus() != OrderStatus.READY) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order is not ready for pickup");
        }
        requirePaidOrder(order, "Online payment must be completed before pickup");
    }

    private void requireDeliverableOrder(Order order) {
        if (order.getStatus() != OrderStatus.PICKED_UP && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order must be picked up before delivery");
        }
        requirePaidOrder(order, "Online payment must be completed before delivery");
    }

    private void requireValidDeliveryStatusUpdate(User user, UUID orderId, Order order, OrderStatus status) {
        if (!List.of(OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED).contains(status)) {
            log.warn("Delivery status update rejected due to invalid target: deliveryPartnerId={}, orderId={}, requestedStatus={}",
                    user.getId(), orderId, status);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid delivery order status");
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Delivery status update rejected because order is closed: deliveryPartnerId={}, orderId={}, currentStatus={}, requestedStatus={}",
                    user.getId(), orderId, order.getStatus(), status);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Order status cannot be changed");
        }
    }

    private void requireDeliveryStatusTransition(Order order, OrderStatus status) {
        if (status == OrderStatus.PICKED_UP) {
            requirePickupReadyOrder(order);
        }
        if (status == OrderStatus.DELIVERED) {
            requireDeliverableOrder(order);
        }
    }

    private void requirePaidOrder(Order order, String message) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    private void requireRole(User user, Role role, String message) {
        if (user.getRole() != role && user.getRole() != Role.ADMIN) {
            log.warn("Order access denied: userId={}, role={}, requiredRole={}", user.getId(), user.getRole(), role);
            throw new BusinessException(ErrorCode.ACCESS_DENIED, message);
        }
    }

    private record PricingSummary(
            AppliedCoupon appliedCoupon,
            BigDecimal tax,
            BigDecimal total) {
    }
}
