package com.pvc.foodie.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.pvc.foodie.feature.auth.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String email)
                        throws UsernameNotFoundException {

                var user = userRepository.findByEmail(email)
                                .orElseThrow(() -> {
                                        log.warn("Security user lookup failed: email={}", email);
                                        return new UsernameNotFoundException("User not found");
                                });

                log.debug("Security user loaded: userId={}, email={}, role={}", user.getId(), user.getEmail(),
                                user.getRole());
                return new org.springframework.security.core.userdetails.User(
                                user.getEmail(),
                                user.getPassword(),
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        }
}
