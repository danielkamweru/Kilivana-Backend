package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.service.KenyaCounty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.kilivana.backend.common.service.UserReferenceCodeGenerator referenceCodeGenerator;
    private final DriverProfileRepository driverProfileRepository;

    @Transactional
    public UserResponse createUser(UserRegistrationRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .phone(phone)
                // Both of these were accepted in the request and then dropped, so an account
                // created from the admin panel arrived with no username or region to display.
                .username(request.getUsername() == null ? null : request.getUsername().trim())
                .region(normaliseRegion(request.getRegion()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .verificationStatus(com.kilivana.backend.common.enums.VerificationStatus.NOT_REQUIRED)
                .referenceCode(referenceCodeGenerator.nextCode(request.getRole()))
                .build();

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return mapToResponse(user);
    }

    public List<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByRole(com.kilivana.backend.common.enums.UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRegistrationRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        String email = normalizeEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(normalizePhone(request.getPhone()));
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String username = request.getUsername().trim();
            boolean takenByAnother = userRepository.existsByUsernameIgnoreCase(username)
                    && !username.equalsIgnoreCase(user.getUsername());
            if (takenByAnother) {
                throw new BadRequestException("Username already exists");
            }
            user.setUsername(username);
        }
        if (request.getRegion() != null) {
            user.setRegion(normaliseRegion(request.getRegion()));
        }
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already exists");
        }
        user.setEmail(email);

        // Admin-set password: only update when a new one is provided, so a profile edit
        // that leaves the password field blank does not wipe the existing hash.
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setStatus(status);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse suspendUser(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A suspension reason is required");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Administrator accounts cannot be suspended from this endpoint");
        }
        user.setStatus(UserStatus.SUSPENDED);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setStatus(UserStatus.ACTIVE);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Profile tables carry no foreign key to users, so deleting the account left the profile
        // behind pointing at a user that no longer exists: an invisible orphan that the roster
        // never shows and nothing ever cleans up. Refusing is the honest answer — an account with
        // a licence and a delivery history behind it is not disposable, and the administrator
        // should deactivate it instead.
        if (user.getRole() == UserRole.DRIVER && driverProfileRepository.existsByUserId(id)) {
            throw new BadRequestException(
                    "This driver has a profile. Delete the driver profile first, or set the account to INACTIVE.");
        }
        userRepository.delete(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
    }

    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone == null ? null : phone.trim()).orElse(null);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    /**
     * The panel offers the 47 Kenyan counties, so a region that is
     * not one of them is refused rather than stored in a spelling
     * the panel will never show again.
     */
    private static String normaliseRegion(String region) {
        if (region == null) {
            return null;
        }
        String county = KenyaCounty.normalise(region);
        if (county == null) {
            throw new BadRequestException("Region must be one of the 47 Kenyan counties");
        }
        return county;
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .username(user.getUsername())
                .referenceCode(user.getReferenceCode())
                .region(user.getRegion())
                .role(user.getRole())
                .status(user.getStatus())
                .verificationStatus(user.getVerificationStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
