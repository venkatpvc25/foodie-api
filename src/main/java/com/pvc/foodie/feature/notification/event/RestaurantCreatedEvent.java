package com.pvc.foodie.feature.notification.event;

import com.pvc.foodie.feature.restaurant.entity.Restaurant;

public record RestaurantCreatedEvent(Restaurant restaurant) {
}
