package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.AdminDriverResponse;
import com.kilivana.backend.admin.dto.DriverCapacityFormatter;
import com.kilivana.backend.admin.dto.DriverRegistrationRequest;
import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.common.service.UserReferenceCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The administrator's driver list.
 *
 * <p>Separate from {@link ProfileService} because this is an administration concern: it reads
 * across users, profiles and delivery jobs, and none of the other roles need that.
 */
@Service
@RequiredArgsConstructor
public class AdminDriverService {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final LogisticsJobRepository logisticsJobRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReferenceCodeGenerator referenceCodeGenerator;

    /**
     * Registers a driver: the login account and the profile in one call.
     *
     * <p>Both rows or neither. Creating the account first and the profile second left a window
     * where a driver existed but had no licence, vehicle or plate, and the roster showed them as
     * a driver with nothing recorded.
     */
    @Transactional
    public AdminDriverResponse registerDriver(DriverRegistrationRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone already exists");
        }
        if (request.getUsername() != null && userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        User user = userRepository.save(User.builder()
                .name(request.getFullName().trim())
                .email(email)
                .phone(phone)
                .username(request.getUsername() == null ? null : request.getUsername().trim())
                .region(request.getRegion())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.DRIVER)
                .status(UserStatus.ACTIVE)
                // KYC is a licence check recorded on the profile, not an email confirmation.
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .referenceCode(referenceCodeGenerator.nextCode(UserRole.DRIVER))
                .build());

        DriverProfile profile = driverProfileRepository.save(DriverProfile.builder()
                .userId(user.getId())
                .address(request.getAddress())
                .licenseNumber(request.getLicenceNumber())
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getPlateNumber().trim().toUpperCase())
                .vehicleMake(request.getVehicleMake())
                .vehicleCapacityKg(DriverCapacityFormatter.toKilograms(request.getVehicleCapacity()))
                .vehicleDetails(request.getVehicleMake() == null || request.getVehicleMake().isBlank()
                        ? null
                        : request.getVehicleMake() + " " + request.getVehicleCapacity())
                .licenseExpiryDate(request.getLicenceExpiry())
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .kycStatus(request.getKycStatus() == null ? KycStatus.PENDING : request.getKycStatus())
                .availabilityStatus(request.getAvailabilityStatus() == null
                        ? DriverStatus.OFFLINE
                        : request.getAvailabilityStatus())
                .build());

        return toResponse(user, profile, Map.of(user.getId(), 0L));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String normalizePhone(String phone) {
        return phone.trim();
    }

    @Transactional(readOnly = true)
    public List<AdminDriverResponse> listDrivers() {
        List<User> drivers = userRepository.findByRole(UserRole.DRIVER);
        Map<Long, DriverProfile> profiles = new HashMap<>();
        for (DriverProfile profile : driverProfileRepository.findAll()) {
            profiles.put(profile.getUserId(), profile);
        }
        Map<Long, Long> deliveredCounts = new HashMap<>();
        for (Object[] row : logisticsJobRepository.countDeliveredByDriver()) {
            deliveredCounts.put((Long) row[0], (Long) row[1]);
        }

        return drivers.stream()
                .map(user -> toResponse(user, profiles.get(user.getId()), deliveredCounts))
                .toList();
    }

    private AdminDriverResponse toResponse(User user, DriverProfile profile, Map<Long, Long> deliveredCounts) {
        Long activeOrderId = logisticsJobRepository.findActiveByDriverId(user.getId()).stream()
                .map(LogisticsJob::getOrderId)
                .findFirst()
                .orElse(null);

        return AdminDriverResponse.builder()
                .userId(user.getId())
                .profileId(profile == null ? null : profile.getId())
                .code(user.getReferenceCode())
                .fullName(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .region(user.getRegion())
                .address(profile == null ? null : profile.getAddress())
                // A driver with no profile is not working, so they read as offline rather than
                // as a missing value the client has to interpret.
                .status(profile == null ? DriverStatus.OFFLINE : profile.getAvailabilityStatus())
                .suspensionReason(profile == null ? null : profile.getSuspensionReason())
                .idType(profile == null ? null : profile.getIdType())
                .idNumber(profile == null ? null : profile.getIdNumber())
                .licenseNumber(profile == null ? null : profile.getLicenseNumber())
                .licenseExpiryDate(profile == null ? null : profile.getLicenseExpiryDate())
                .kycStatus(profile == null ? KycStatus.PENDING : profile.getKycStatus())
                .vehicleType(profile == null ? null : profile.getVehicleType())
                .vehicleCapacity(profile == null ? null : DriverCapacityFormatter.format(profile.getVehicleCapacityKg()))
                .plateNumber(profile == null ? null : profile.getVehicleNumber())
                .vehicleMake(profile == null ? null : profile.getVehicleMake())
                .activeOrderId(activeOrderId)
                .totalDeliveries(deliveredCounts.getOrDefault(user.getId(), 0L))
                // No rating source exists in the system; left null rather than invented.
                .rating(null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<AdminDriverResponse> findDriver(Long userId) {
        Optional<User> user = userRepository.findById(userId)
                .filter(candidate -> candidate.getRole() == UserRole.DRIVER);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        long delivered = logisticsJobRepository.countDeliveredByDriver().stream()
                .filter(row -> userId.equals(row[0]))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);
        return user.map(candidate -> toResponse(
                candidate,
                driverProfileRepository.findByUserId(userId).orElse(null),
                Map.of(userId, delivered)));
    }
}