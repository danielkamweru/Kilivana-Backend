package com.kilivana.backend.config;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.service.UserReferenceCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial administrator without loading the demo catalogue, orders, or users.
 *
 * <p>Enable this only for the deployment which needs its first administrator with
 * {@code SEED_ADMIN_ENABLED=true}. It is deliberately separate from {@code SEED_DATA},
 * which loads the complete disposable demo dataset.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.admin-enabled", havingValue = "true")
public class AdminBootstrapSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReferenceCodeGenerator referenceCodeGenerator;

    @Value("${app.seed.enabled:false}")
    private boolean demoSeedEnabled;

    @Value("${app.seed.admin-email:admin@kilivana.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.enforce-passwords:false}")
    private boolean enforcePasswords;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdmin() {
        if (demoSeedEnabled) {
            log.info("Skipping admin-only seed because the full demo seeder is enabled.");
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("SEED_ADMIN_ENABLED is true but SEED_ADMIN_PASSWORD is blank; no administrator was created.");
            return;
        }

        String email = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
        var existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getRole() != UserRole.ADMIN) {
                log.error("Refusing to promote existing non-admin account {} through environment seeding.", email);
                return;
            }
            if (enforcePasswords && !passwordEncoder.matches(adminPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(adminPassword));
                userRepository.save(user);
                log.info("Reset password for existing seeded administrator {}.", email);
            } else {
                log.info("Administrator {} already exists; no change made.", email);
            }
            return;
        }

        User admin = User.builder()
                .name("Kilivana Admin")
                .email(email)
                .phone("+254700000001")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .region("Nairobi")
                .referenceCode(referenceCodeGenerator.nextCode(UserRole.ADMIN))
                .build();
        admin.setEmailVerified(true);
        userRepository.save(admin);
        log.info("Created initial administrator {}.", email);
    }
}
