package com.kilivana.backend.config;

import com.kilivana.backend.admin.entity.BuyerProfile;
import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.admin.entity.FarmerProfile;
import com.kilivana.backend.admin.entity.InspectorProfile;
import com.kilivana.backend.admin.entity.SupplierProfile;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.BuyerProfileRepository;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.FarmerProfileRepository;
import com.kilivana.backend.admin.repository.InspectorProfileRepository;
import com.kilivana.backend.admin.repository.SupplierProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.enums.VehicleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private final FarmerProfileRepository farmerProfileRepository;
    private final BuyerProfileRepository buyerProfileRepository;
    private final SupplierProfileRepository supplierProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final InspectorProfileRepository inspectorProfileRepository;

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("admin.test@kilivana.local", "Admin Test", "0700000001", UserRole.ADMIN, null),
            new SeedUser("farmer.test@kilivana.local", "Farmer Test", "0700000002", UserRole.FARMER, null),
            new SeedUser("buyer.test@kilivana.local", "Buyer Test", "0700000003", UserRole.BUYER, null),
            new SeedUser("supplier.test@kilivana.local", "Supplier Test", "0700000004", UserRole.SUPPLIER, null),
            new SeedUser("driver.test@kilivana.local", "Driver Test", "0700000005", UserRole.DRIVER, null),
            new SeedUser("inspector.test@kilivana.local", "Inspector Test", "0700000006", UserRole.INSPECTOR, null),
            // The admin panel hardcodes this address in its mock auth service, so a developer
            // signing in to the panel needs the backend to accept the same pair. The password
            // differs from the shared seed password, hence the override.
            new SeedUser("admin@kilivana.com", "Kilivana Admin", "0700000088", UserRole.ADMIN, "Admin@123"));

    /**
     * @param password the shared seed password when null, otherwise this account's own password
     */
    private record SeedUser(String email, String name, String phone, UserRole role, String password) {
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        String password = System.getProperty("app.dev-seed.password", "Kilivana#2026");
        int created = 0;
        int repaired = 0;

        for (SeedUser seed : SEED_USERS) {
            User existing = userRepository.findByEmailIgnoreCase(seed.email()).orElse(null);

            if (existing == null) {
                userRepository.save(User.builder()
                        .name(seed.name())
                        .email(seed.email())
                        .phone(seed.phone())
                        .passwordHash(passwordEncoder.encode(
                                seed.password() == null ? password : seed.password()))
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

        seedProfiles();

        log.info("Development seed complete: {} account(s) created, {} repaired.", created, repaired);
    }

    /**
     * Gives each seeded role account its profile row.
     *
     * Without this, every profile endpoint returns 404 for the documented credentials, which
     * reads as a broken backend when it is only missing data. The role profile screens are
     * the first thing anyone demonstrates, so they should have something to show.
     */
    private void seedProfiles() {
        firstUserWithRole(UserRole.FARMER).ifPresent(user -> farmerProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> farmerProfileRepository.save(FarmerProfile.builder()
                        .userId(user.getId())
                        .farmName("Green Valley Farm")
                        .location("Nakuru, Kenya")
                        .build())));

        firstUserWithRole(UserRole.BUYER).ifPresent(user -> buyerProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> buyerProfileRepository.save(BuyerProfile.builder()
                        .userId(user.getId())
                        .contactDetails("+254700000003")
                        .build())));

        firstUserWithRole(UserRole.SUPPLIER).ifPresent(user -> supplierProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> supplierProfileRepository.save(SupplierProfile.builder()
                        .userId(user.getId())
                        .businessName("Kilivana Supplies Ltd")
                        .location("Nairobi, Kenya")
                        .contractEndDate(LocalDate.now().plusYears(1))
                        .build())));

        firstUserWithRole(UserRole.DRIVER).ifPresent(user -> driverProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> driverProfileRepository.save(DriverProfile.builder()
                        .userId(user.getId())
                        .address("Nairobi, Kenya")
                        .licenseNumber("KDL-DEV-0001")
                        .vehicleType(VehicleType.TRUCK)
                        .vehicleNumber("KDB 432A")
                        .vehicleMake("Isuzu")
                        .vehicleCapacityKg(1500)
                        .licenseExpiryDate(LocalDate.now().plusYears(2))
                        .idType("National ID")
                        .idNumber("29584712")
                        .kycStatus(KycStatus.VERIFIED)
                        .availabilityStatus(DriverStatus.AVAILABLE)
                        .build())));

        firstUserWithRole(UserRole.INSPECTOR).ifPresent(user -> inspectorProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> inspectorProfileRepository.save(InspectorProfile.builder()
                        .userId(user.getId())
                        .assignedArea("Nakuru")
                        .status("active")
                        .build())));
    }

    private Optional<User> firstUserWithRole(UserRole role) {
        return userRepository.findByRole(role).stream().findFirst();
    }
}
