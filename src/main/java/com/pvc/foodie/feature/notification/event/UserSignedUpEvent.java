package com.pvc.foodie.feature.notification.event;

import com.pvc.foodie.feature.auth.entity.User;

public record UserSignedUpEvent(User user) {
}
