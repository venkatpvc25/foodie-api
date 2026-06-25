package com.pvc.foodie.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.pvc.foodie.config.AdminProperties;
import com.pvc.foodie.feature.auth.entity.Role;
import com.pvc.foodie.feature.auth.entity.User;
import com.pvc.foodie.feature.auth.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(String... args) {

        if (!adminProperties.isEnabled()) {
            log.info("Admin seeding skipped: enabled=false");
            return;
        }

        userRepository.findByEmail(adminProperties.getEmail())
                .ifPresentOrElse(
                        user -> {
                            log.info("Admin user already exists: userId={}, email={}", user.getId(), user.getEmail());
                        },
                        () -> {
                            User admin = new User();
                            admin.setName("Admin");
                            admin.setEmail(adminProperties.getEmail());
                            admin.setPassword(
                                    passwordEncoder.encode(adminProperties.getPassword()));
                            admin.setRole(Role.ADMIN);

                            User saved = userRepository.save(admin);
                            log.info("Admin user seeded: userId={}, email={}", saved.getId(), saved.getEmail());
                        });
    }
}
