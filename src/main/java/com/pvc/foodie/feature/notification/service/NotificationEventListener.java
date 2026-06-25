package com.pvc.foodie.feature.notification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.auth.repository.UserRepository;
import com.pvc.foodie.feature.notification.entity.Notification;
import com.pvc.foodie.feature.notification.event.DeliveryOrderClaimedEvent;
import com.pvc.foodie.feature.notification.event.DeliveryPayoutTransferredEvent;
import com.pvc.foodie.feature.notification.event.OrderPlacedEvent;
import com.pvc.foodie.feature.notification.event.OrderStatusChangedEvent;
import com.pvc.foodie.feature.notification.event.PaymentVerifiedEvent;
import com.pvc.foodie.feature.notification.event.RestaurantCreatedEvent;
import com.pvc.foodie.feature.notification.event.UserSignedUpEvent;
import com.pvc.foodie.feature.notification.repository.NotificationRepository;
import com.pvc.foodie.feature.order.entity.Order;
import com.pvc.foodie.feature.order.entity.OrderStatus;
import com.pvc.foodie.feature.order.entity.PaymentStatus;
import com.pvc.foodie.feature.restaurant.entity.Restaurant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final String NEW_DELIVERY_ORDER_TITLE = "New delivery order";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FirebasePushNotificationService firebasePushNotificationService;

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        Order order = event.order();
        notifyUser(
                order.getRestaurant().getCreatedBy(),
                "New order received",
                "Order " + order.getOrderNumber() + " has been placed.",
                "ORDER_PLACED",
                "ORDER",
                order.getId(),
                restaurantOrderRoute(order));
    }

    @EventListener
    public void onPaymentVerified(PaymentVerifiedEvent event) {
        Order order = event.order();
        notifyUser(
                order.getUser(),
                "Payment confirmed",
                "Payment for order " + order.getOrderNumber() + " is confirmed.",
                "PAYMENT_VERIFIED",
                "ORDER",
                order.getId(),
                customerOrderRoute(order));
        notifyUser(
                order.getRestaurant().getCreatedBy(),
                "Order payment confirmed",
                "Payment for order " + order.getOrderNumber() + " is confirmed.",
                "PAYMENT_VERIFIED",
                "ORDER",
                order.getId(),
                restaurantOrderRoute(order));
        notifyDeliveryPartnersIfReady(order);
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        Order order = event.order();
        if (event.oldStatus() == event.newStatus()) {
            return;
        }

        switch (event.newStatus()) {
            case ACCEPTED -> notifyUser(
                    order.getUser(),
                    "Order accepted",
                    "Your order " + order.getOrderNumber() + " has been accepted.",
                    "ORDER_ACCEPTED",
                    "ORDER",
                    order.getId(),
                    customerOrderRoute(order));
            case PREPARING -> notifyUser(
                    order.getUser(),
                    "Order preparing",
                    "Your order " + order.getOrderNumber() + " is being prepared.",
                    "ORDER_PREPARING",
                    "ORDER",
                    order.getId(),
                    customerOrderRoute(order));
            case READY -> {
                notifyUser(
                        order.getUser(),
                        "Order ready",
                        "Your order " + order.getOrderNumber() + " is ready for pickup.",
                        "ORDER_READY",
                        "ORDER",
                        order.getId(),
                        customerOrderRoute(order));
                notifyDeliveryPartnersIfReady(order);
            }
            case PICKED_UP -> {
                notifyUser(
                        order.getUser(),
                        "Order picked up",
                        "Your order " + order.getOrderNumber() + " has been picked up.",
                        "ORDER_PICKED_UP",
                        "ORDER",
                        order.getId(),
                        customerOrderRoute(order));
                notifyUser(
                        order.getRestaurant().getCreatedBy(),
                        "Order picked up",
                        "Order " + order.getOrderNumber() + " has been picked up by the delivery partner.",
                        "ORDER_PICKED_UP",
                        "ORDER",
                        order.getId(),
                        restaurantOrderRoute(order));
            }
            case OUT_FOR_DELIVERY -> notifyUser(
                    order.getUser(),
                    "Out for delivery",
                    "Your order " + order.getOrderNumber() + " is out for delivery.",
                    "ORDER_OUT_FOR_DELIVERY",
                    "ORDER",
                    order.getId(),
                    customerOrderRoute(order));
            case DELIVERED -> {
                notifyUser(
                        order.getUser(),
                        "Order delivered",
                        "Your order " + order.getOrderNumber() + " has been delivered.",
                        "ORDER_DELIVERED",
                        "ORDER",
                        order.getId(),
                        customerOrderRoute(order));
                notifyUser(
                        order.getRestaurant().getCreatedBy(),
                        "Order delivered",
                        "Order " + order.getOrderNumber() + " has been delivered.",
                        "ORDER_DELIVERED",
                        "ORDER",
                        order.getId(),
                        restaurantOrderRoute(order));
                notifyDeliveryPartner(
                        order,
                        "Delivery completed",
                        "Order " + order.getOrderNumber() + " has been marked delivered.",
                        "ORDER_DELIVERED",
                        deliveryOrderRoute(order));
            }
            case CANCELLED -> notifyCancellation(order);
            default -> {
            }
        }
    }

    @EventListener
    public void onDeliveryOrderClaimed(DeliveryOrderClaimedEvent event) {
        Order order = event.order();
        String deliveryPartnerName = order.getDeliveryPartner() == null ? "A delivery partner"
                : order.getDeliveryPartner().getName();
        notifyUser(
                order.getUser(),
                "Delivery partner assigned",
                deliveryPartnerName + " claimed order " + order.getOrderNumber() + ".",
                "ORDER_CLAIMED",
                "ORDER",
                order.getId(),
                customerOrderRoute(order));
        notifyUser(
                order.getRestaurant().getCreatedBy(),
                "Order claimed for delivery",
                deliveryPartnerName + " claimed order " + order.getOrderNumber() + ".",
                "ORDER_CLAIMED",
                "ORDER",
                order.getId(),
                restaurantOrderRoute(order));
    }

    @EventListener
    public void onDeliveryPayoutTransferred(DeliveryPayoutTransferredEvent event) {
        Order order = event.order();
        notifyDeliveryPartner(
                order,
                "Delivery payout transferred",
                "Delivery payout for order " + order.getOrderNumber() + " has been transferred.",
                "DELIVERY_PAYOUT_TRANSFERRED",
                deliveryOrderRoute(order));
    }

    @EventListener
    public void onUserSignedUp(UserSignedUpEvent event) {
        User user = event.user();
        notifyUser(
                user,
                "Welcome to Foodie",
                "Your " + roleLabel(user.getRole()) + " account is ready.",
                "USER_SIGNED_UP",
                "USER",
                user.getId(),
                "/profile");
        if (user.getRole() == Role.RESTAURANT || user.getRole() == Role.DELIVERY_PARTNER) {
            notifyAdmins(
                    "New " + roleLabel(user.getRole()) + " signup",
                    user.getName() + " signed up as " + roleLabel(user.getRole()) + ".",
                    "USER_SIGNED_UP",
                    "USER",
                    user.getId(),
                    "/admin/users/" + user.getId());
        }
    }

    @EventListener
    public void onRestaurantCreated(RestaurantCreatedEvent event) {
        Restaurant restaurant = event.restaurant();
        notifyAdmins(
                "Restaurant created",
                restaurant.getName() + " was created by " + restaurant.getCreatedBy().getName() + ".",
                "RESTAURANT_CREATED",
                "RESTAURANT",
                restaurant.getId(),
                "/admin/restaurants/" + restaurant.getId());
        notifyUser(
                restaurant.getCreatedBy(),
                "Restaurant created",
                restaurant.getName() + " has been created.",
                "RESTAURANT_CREATED",
                "RESTAURANT",
                restaurant.getId(),
                "/restaurant/restaurants/" + restaurant.getId());
    }

    private void notifyDeliveryPartnersIfReady(Order order) {
        if (order.getStatus() != OrderStatus.READY || order.getPaymentStatus() != PaymentStatus.PAID
                || order.getDeliveryPartner() != null) {
            return;
        }
        notifyUsers(
                userRepository.findByRole(Role.DELIVERY_PARTNER),
                NEW_DELIVERY_ORDER_TITLE,
                "Order " + order.getOrderNumber() + " is ready to claim.",
                "DELIVERY_ORDER_AVAILABLE",
                "ORDER",
                order.getId(),
                "/delivery/available");
    }

    private void notifyCancellation(Order order) {
        notifyUser(
                order.getUser(),
                "Order cancelled",
                "Order " + order.getOrderNumber() + " has been cancelled.",
                "ORDER_CANCELLED",
                "ORDER",
                order.getId(),
                customerOrderRoute(order));
        notifyUser(
                order.getRestaurant().getCreatedBy(),
                "Order cancelled",
                "Order " + order.getOrderNumber() + " has been cancelled.",
                "ORDER_CANCELLED",
                "ORDER",
                order.getId(),
                restaurantOrderRoute(order));
        notifyDeliveryPartner(
                order,
                "Order cancelled",
                "Order " + order.getOrderNumber() + " has been cancelled.",
                "ORDER_CANCELLED",
                deliveryOrderRoute(order));
    }

    private void notifyAdmins(String title, String message, String type, String targetType, UUID targetId, String route) {
        notifyUsers(userRepository.findByRole(Role.ADMIN), title, message, type, targetType, targetId, route);
    }

    private void notifyDeliveryPartner(Order order, String title, String message, String type, String route) {
        if (order.getDeliveryPartner() != null) {
            notifyUser(order.getDeliveryPartner(), title, message, type, "ORDER", order.getId(), route);
        }
    }

    private void notifyUsers(
            List<User> users,
            String title,
            String message,
            String type,
            String targetType,
            UUID targetId,
            String route) {
        users.forEach(user -> notifyUser(user, title, message, type, targetType, targetId, route));
    }

    private void notifyUser(
            User user,
            String title,
            String message,
            String type,
            String targetType,
            UUID targetId,
            String route) {
        if (user == null || notificationRepository.existsByUserIdAndTitleAndMessage(user.getId(), title, message)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setRoute(route);
        notificationRepository.save(notification);
        firebasePushNotificationService.sendToUser(user, notification);
        log.info("Notification created: userId={}, title={}", user.getId(), title);
    }

    private String customerOrderRoute(Order order) {
        return "/orders/" + order.getId();
    }

    private String restaurantOrderRoute(Order order) {
        return "/restaurant/orders/" + order.getId();
    }

    private String deliveryOrderRoute(Order order) {
        return "/delivery/orders/" + order.getId();
    }

    private String roleLabel(Role role) {
        return role.name().toLowerCase().replace('_', ' ');
    }
}
