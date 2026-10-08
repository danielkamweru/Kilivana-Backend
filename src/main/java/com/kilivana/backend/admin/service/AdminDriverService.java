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
import com.kilivana.backend.common.exception.ConflictException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.common.service.KenyaCounty;
import com.kilivana.backend.common.service.KenyanIdType;
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

        // The panel offers a Kenyan county and a Kenyan identity document. Accepting the
        // region as free text meant a driver could be filed under a county that does not
        // exist here, which then never matches anything the reports group by.
        String county = KenyaCounty.normalise(request.getRegion());
        if (county == null) {
            throw new BadRequestException("Region must be one of the 47 Kenyan counties");
        }
        String idType = KenyanIdType.normalise(request.getIdType());
        if (idType == null) {
            throw new BadRequestException("ID type must be one of: "
                    + String.join(", ", KenyanIdType.all()));
        }

        User user = userRepository.save(User.builder()
                .name(request.getFullName().trim())
                .email(email)
                .phone(phone)
                .username(request.getUsername() == null ? null : request.getUsername().trim())
                .region(county)
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
                .idType(idType)
                .idNumber(request.getIdNumber())
                .kycStatus(request.getKycStatus() == null ? KycStatus.PENDING : request.getKycStatus())
                .availabilityStatus(request.getAvailabilityStatus() == null
                        ? DriverStatus.OFFLINE
                        : request.getAvailabilityStatus())
                .build());

        return toResponse(user, profile, Map.of(user.getId(), 0L));
    }

    @Transactional
    public AdminDriverResponse updateDriver(Long userId, DriverRegistrationRequest request) {
        User user = driverUser(userId);
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Driver has no profile to edit"));

        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered to another account", "EMAIL_TAKEN");
        }
        if (!user.getPhone().equals(phone) && userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number already registered to another account", "PHONE_TAKEN");
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().trim().equalsIgnoreCase(user.getUsername())
                && userRepository.existsByUsernameIgnoreCase(request.getUsername().trim())) {
            throw new ConflictException("Username already taken", "USERNAME_TAKEN");
        }

        if (request.getRegion() != null && !request.getRegion().isBlank()) {
            String county = KenyaCounty.normalise(request.getRegion());
            if (county == null) {
                throw new BadRequestException("Region must be one of the 47 Kenyan counties");
            }
            user.setRegion(county);
        }

        user.setName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setUsername(request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername().trim() : null);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        String idType = KenyanIdType.normalise(request.getIdType());
        if (idType == null) {
            throw new BadRequestException("ID type must be one of: "
                    + String.join(", ", KenyanIdType.all()));
        }

        profile.setAddress(request.getAddress());
        profile.setLicenseNumber(request.getLicenceNumber());
        profile.setVehicleType(request.getVehicleType());
        profile.setVehicleNumber(request.getPlateNumber().trim().toUpperCase());
        profile.setVehicleMake(request.getVehicleMake());
        profile.setVehicleCapacityKg(DriverCapacityFormatter.toKilograms(request.getVehicleCapacity()));
        profile.setVehicleDetails(request.getVehicleMake() == null || request.getVehicleMake().isBlank()
                ? null
                : request.getVehicleMake() + " " + request.getVehicleCapacity());
        profile.setLicenseExpiryDate(request.getLicenceExpiry());
        profile.setIdType(idType);
        profile.setIdNumber(request.getIdNumber());
        profile.setKycStatus(request.getKycStatus() == null ? KycStatus.PENDING : request.getKycStatus());
        if (request.getAvailabilityStatus() != null) {
            profile.setAvailabilityStatus(request.getAvailabilityStatus());
        }

        User savedUser = userRepository.save(user);
        DriverProfile savedProfile = driverProfileRepository.save(profile);
        return toResponse(savedUser, savedProfile, Map.of(savedUser.getId(), 0L));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String normalizePhone(String phone) {
        return phone.trim();
    }

    /**
     * Deletes a driver: the profile first, then the account.
     *
     * <p>Ordered that way because the profile table has no foreign key, so deleting the account
     * first would leave the profile pointing at nothing.
     *
     * @throws BadRequestException when the driver has delivery history
     */
    @Transactional
    public void deleteDriver(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Driver", userId);
        }
        boolean hasHistory = !logisticsJobRepository.findByDriverId(userId).isEmpty();
        if (hasHistory) {
            throw new BadRequestException(
                    "This driver has delivery history and cannot be deleted. Set the account to INACTIVE instead.");
        }
        driverProfileRepository.findByUserId(userId).ifPresent(driverProfileRepository::delete);
        userRepository.deleteById(userId);
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

    /**
     * Suspends a driver: the account stays, but the roster
     * reads them as suspended and the reason is kept on the
     * profile for the panel to show.
     */
    @Transactional
    public AdminDriverResponse suspendDriver(Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A suspension reason is required when a driver is suspended");
        }
        User user = driverUser(userId);
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(
                        "Driver " + userId + " has no profile to suspend"));
        profile.setAvailabilityStatus(DriverStatus.SUSPENDED);
        profile.setSuspensionReason(reason.trim());
        driverProfileRepository.save(profile);
        return findDriver(userId).orElseThrow();
    }

    /** Lifts a suspension and puts the driver back online. */
    @Transactional
    public AdminDriverResponse unsuspendDriver(Long userId) {
        driverUser(userId);
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(
                        "Driver " + userId + " has no profile to reinstate"));
        profile.setAvailabilityStatus(DriverStatus.AVAILABLE);
        profile.setSuspensionReason(null);
        driverProfileRepository.save(profile);
        return findDriver(userId).orElseThrow();
    }

    /** The user, proven to be a driver account. */
    private User driverUser(Long userId) {
        return userRepository.findById(userId)
                .filter(candidate -> candidate.getRole() == UserRole.DRIVER)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", userId));
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