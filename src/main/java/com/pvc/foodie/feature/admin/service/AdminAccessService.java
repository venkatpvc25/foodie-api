package com.pvc.foodie.feature.admin.service;

import org.springframework.stereotype.Service;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.security.CurrentUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private final CurrentUserService currentUserService;

    public User requireAdmin() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            log.warn("Admin access denied: userId={}, role={}", user.getId(), user.getRole());
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Admin access required");
        }
        return user;
    }
}
