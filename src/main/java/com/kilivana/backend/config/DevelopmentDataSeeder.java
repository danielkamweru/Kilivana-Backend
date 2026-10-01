package com.kilivana.backend.config;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates the local development test accounts so the API can be exercised end to end
 * without a manual bootstrap step.
 *
 * <p>Disabled unless {@code app.dev-seed.enabled=true}. It must never be enabled in a
 * deployed environment: it creates accounts with a known password. Passwords are stored
 * through the application's {@link PasswordEncoder}, so the rows are ordinary BCrypt
 * hashes and the accounts authenticate through the normal login endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev-seed.enabled", havingValue = "true")
public class DevelopmentDataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("admin.test@kilivana.local", "Admin Test", "0700000001", UserRole.ADMIN),
            new SeedUser("superadmin.test@kilivana.local", "Super Admin Test", "0700000007", UserRole.SUPER_ADMIN),
            new SeedUser("farmer.test@kilivana.local", "Farmer Test", "0700000002", UserRole.FARMER),
            new SeedUser("buyer.test@kilivana.local", "Buyer Test", "0700000003", UserRole.BUYER),
            new SeedUser("supplier.test@kilivana.local", "Supplier Test", "0700000004", UserRole.SUPPLIER),
            new SeedUser("driver.test@kilivana.local", "Driver Test", "0700000005", UserRole.DRIVER),
            new SeedUser("inspector.test@kilivana.local", "Inspector Test", "0700000006", UserRole.INSPECTOR));

    private record SeedUser(String email, String name, String phone, UserRole role) {
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        String password = System.getProperty("app.dev-seed.password", "Kilivana#2026");
        int created = 0;
        int repaired = 0;

        for (SeedUser seed : SEED_USERS) {
            User existing = userRepository.findByEmail(seed.email()).orElse(null);

            if (existing == null) {
                userRepository.save(User.builder()
                        .name(seed.name())
                        .email(seed.email())
                        .phone(seed.phone())
                        .passwordHash(passwordEncoder.encode(password))
                        .role(seed.role())
                        .status(UserStatus.ACTIVE)
                        .verificationStatus(VerificationStatus.NOT_REQUIRED)
                        .build());
                created++;
                continue;
            }

            // Never touch accounts that are not ours, and never silently rewrite a
            // password. Only repair role/status so the documented credentials keep working.
            if (existing.getRole() != seed.role() || existing.getStatus() != UserStatus.ACTIVE) {
                existing.setRole(seed.role());
                existing.setStatus(UserStatus.ACTIVE);
                userRepository.save(existing);
                repaired++;
            }
        }

        log.info("Development seed complete: {} account(s) created, {} repaired.", created, repaired);
    }
}
