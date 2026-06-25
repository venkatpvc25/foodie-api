package com.pvc.foodie.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.pvc.foodie.comman.exception.BusinessException;
import com.pvc.foodie.comman.exception.ErrorCode;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.auth.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authenticated user not found in database: email={}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found");
                });
    }
}
