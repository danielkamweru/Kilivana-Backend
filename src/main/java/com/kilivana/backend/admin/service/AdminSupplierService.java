package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.AdminSupplierResponse;
import com.kilivana.backend.admin.dto.SupplierRegistrationRequest;
import com.kilivana.backend.admin.entity.SupplierProfile;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.SupplierProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ConflictException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.service.KenyaCounty;
import com.kilivana.backend.common.service.UserReferenceCodeGenerator;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The administrator's supplier list.
 *
 * <p>Separate from {@link ProfileService} because this is an administration concern: it reads
 * across users, profiles and products, and none of the other roles need that.
 */
@Service
@RequiredArgsConstructor
public class AdminSupplierService {

    private final UserRepository userRepository;
    private final SupplierProfileRepository supplierProfileRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReferenceCodeGenerator referenceCodeGenerator;

    /**
     * Registers a supplier: the login account and the profile in one call.
     *
     * <p>Both rows or neither. Creating the account first and the profile second left a window
     * where a supplier existed but had no profile, and the roster showed them as a supplier
     * with nothing recorded.
     */
    @Transactional
    public AdminSupplierResponse registerSupplier(SupplierRegistrationRequest request) {
        validateSupplierRequest(request, true);

        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered", "EMAIL_TAKEN");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number already registered", "PHONE_TAKEN");
        }
        if (request.getUsername() != null && userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new ConflictException("Username already taken", "USERNAME_TAKEN");
        }

        String county = KenyaCounty.normalise(request.getRegion());
        if (county == null) {
            throw new BadRequestException("Region must be one of the 47 Kenyan counties");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required when creating a supplier");
        }

        UserStatus userStatus = parseStatus(request.getStatus(), UserStatus.PENDING_VERIFICATION);

        User user = userRepository.save(User.builder()
                .name(request.getContactPerson().trim())
                .email(email)
                .phone(phone)
                .username(request.getUsername() != null ? request.getUsername().trim() : null)
                .region(county)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.SUPPLIER)
                .status(userStatus)
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .referenceCode(referenceCodeGenerator.nextCode(UserRole.SUPPLIER))
                .build());

        SupplierProfile profile = supplierProfileRepository.save(SupplierProfile.builder()
                .userId(user.getId())
                .businessName(request.getCompanyName().trim())
                .location(county)
                .address(request.getAddress())
                .category(request.getCategory())
                .contractEndDate(request.getContractEndDate())
                .build());

        return toResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public List<AdminSupplierResponse> listSuppliers() {
        List<User> suppliers = userRepository.findByRole(UserRole.SUPPLIER);
        Map<Long, SupplierProfile> profiles = new HashMap<>();
        for (SupplierProfile profile : supplierProfileRepository.findAll()) {
            profiles.put(profile.getUserId(), profile);
        }

        return suppliers.stream()
                .map(user -> toResponse(user, profiles.get(user.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminSupplierResponse getSupplier(Long userId) {
        User user = supplierUser(userId);
        SupplierProfile profile = supplierProfileRepository.findByUserId(userId).orElse(null);
        return toResponse(user, profile);
    }

    @Transactional
    public AdminSupplierResponse updateSupplier(Long userId, SupplierRegistrationRequest request) {
        validateSupplierRequest(request, false);

        User user = supplierUser(userId);
        SupplierProfile profile = supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Supplier has no profile to edit"));

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

        String county = KenyaCounty.normalise(request.getRegion());
        if (county == null) {
            throw new BadRequestException("Region must be one of the 47 Kenyan counties");
        }

        user.setName(request.getContactPerson().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setUsername(request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername().trim() : null);
        user.setRegion(county);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            // Password is optional on update: omit it to leave the existing password unchanged.
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(parseStatus(request.getStatus(), user.getStatus()));
        }

        profile.setBusinessName(request.getCompanyName().trim());
        profile.setLocation(county);
        profile.setAddress(request.getAddress());
        profile.setCategory(request.getCategory());
        profile.setContractEndDate(request.getContractEndDate());

        User savedUser = userRepository.save(user);
        SupplierProfile savedProfile = supplierProfileRepository.save(profile);
        return toResponse(savedUser, savedProfile);
    }

    private static void validateSupplierRequest(SupplierRegistrationRequest request, boolean isCreate) {
        if (isCreate && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new BadRequestException("Password is required when creating a supplier");
        }
    }

    private static UserStatus parseStatus(String status, UserStatus defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }
        return switch (status.toLowerCase()) {
            case "active" -> UserStatus.ACTIVE;
            case "pending" -> UserStatus.PENDING_VERIFICATION;
            case "suspended" -> UserStatus.SUSPENDED;
            default -> defaultStatus;
        };
    }

    /**
     * Deletes a supplier: the profile first, then the account.
     *
     * @throws BadRequestException when the supplier has products (cannot delete).
     */
    @Transactional
    public void deleteSupplier(Long userId) {
        supplierUser(userId);
        // Refused while the supplier still has products listed, because those products are
        // part of the marketplace and deleting the account would orphan them.
        var products = productRepository.findBySellerIdAndSellerType(userId, SellerType.SUPPLIER);
        if (!products.isEmpty()) {
            throw new BadRequestException(
                    "This supplier has " + products.size() + " product(s) and cannot be deleted. "
                    + "Reassign or delete the products first, or set the account to INACTIVE.");
        }
        supplierProfileRepository.findByUserId(userId)
                .ifPresent(supplierProfileRepository::delete);
        userRepository.deleteById(userId);
    }

    @Transactional
    public AdminSupplierResponse suspendSupplier(Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A suspension reason is required");
        }
        User user = supplierUser(userId);
        supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Supplier has no profile to suspend"));
        user.setStatus(UserStatus.SUSPENDED);
        user.setSuspensionReason(reason.trim());
        user.setStatusChangedAt(LocalDateTime.now());
        userRepository.save(user);
        return getSupplier(userId);
    }

    @Transactional
    public AdminSupplierResponse unsuspendSupplier(Long userId) {
        User user = supplierUser(userId);
        supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Supplier has no profile to reinstate"));
        user.setStatus(UserStatus.ACTIVE);
        user.setSuspensionReason(null);
        user.setStatusChangedAt(LocalDateTime.now());
        userRepository.save(user);
        return getSupplier(userId);
    }

    private User supplierUser(Long userId) {
        return userRepository.findById(userId)
                .filter(candidate -> candidate.getRole() == UserRole.SUPPLIER)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", userId));
    }

    private AdminSupplierResponse toResponse(User user, SupplierProfile profile) {
        int productsCount = profile == null
                ? 0
                : productRepository.findBySellerIdAndSellerType(user.getId(), SellerType.SUPPLIER).size();

        return AdminSupplierResponse.builder()
                .userId(user.getId())
                .profileId(profile == null ? null : profile.getId())
                .code(user.getReferenceCode())
                .companyName(profile == null ? null : profile.getBusinessName())
                .contactPerson(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .region(user.getRegion())
                .address(profile == null ? null : profile.getAddress())
                .category(profile == null ? null : profile.getCategory())
                .contractEndDate(profile == null ? null : profile.getContractEndDate())
                .status(AdminSupplierResponse.mapStatus(user.getStatus(), user.getVerificationStatus()))
                .suspensionReason(user.getSuspensionReason())
                .productsCount(productsCount)
                .rating(null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }
}
