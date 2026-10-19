package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.AuthLoginRequest;
import com.kilivana.backend.common.dto.AuthTokenResponse;
import com.kilivana.backend.common.dto.PasswordResetRequest;
import com.kilivana.backend.common.entity.PasswordResetToken;
import com.kilivana.backend.common.repository.PasswordResetTokenRepository;
import com.kilivana.backend.mail.SendGridMailService;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ConflictException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.exception.UnauthorizedException;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.service.KenyaCounty;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Self-service registration, login, token refresh and password reset.
 *
 * <p>Staff roles (ADMIN, INSPECTOR) cannot self-register; they are created by an existing
 * administrator through the admin panel. Passwords are never returned; only JWT access and
 * refresh tokens are issued. The access token expiry is configurable; a non-positive value
 * means the token carries no expiry and stays valid until the signing secret changes.
 */
@Service
    @RequiredArgsConstructor
    public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserReferenceCodeGenerator referenceCodeGenerator;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SendGridMailService sendGridMailService;

    @Value("${app.password-reset.frontend-url:}")
    private String passwordResetFrontendUrl;
    @Value("${app.password-reset.token-expiry-minutes:30}")
    private long passwordResetExpiryMinutes;

    private static final SecureRandom RESET_RANDOM = new SecureRandom();

    /**
     * Self-service registration. Refuses staff roles, rejects an email or phone already
     * taken, and normalizes the email, phone and county before the row is written.
     */
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        if (request.getRole() != null && request.getRole().isStaff()) {
            throw new BadRequestException(
                    "Administrator accounts cannot be self-registered");
        }
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already exists, please log in instead");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number already exists, please log in instead");
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .phone(phone)
                .username(request.getUsername())
                .region(normaliseRegion(request.getRegion()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .referenceCode(referenceCodeGenerator.nextCode(request.getRole()))
                .build();

        return mapToResponse(userRepository.save(user));
    }

    /**
     * Authenticates with email and password. A wrong password and an unknown email produce
     * the same failure so accounts cannot be enumerated; suspended and inactive accounts
     * are refused.
     */
    public AuthTokenResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedException("This account is " + user.getStatus().name().toLowerCase());
        }

        return buildTokenResponse(user);
    }

    private AuthTokenResponse buildTokenResponse(User user) {
        long expirationHours = jwtProperties.getAccessTokenExpirationHours();
        return AuthTokenResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                // null means the token carries no expiry and stays valid until the
                // signing secret changes.
                .expiresIn(expirationHours > 0 ? Duration.ofHours(expirationHours).toSeconds() : null)
                .user(mapToResponse(user))
                .build();
    }

    /** Returns the profile of the user the bearer token identifies. */
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToResponse(user);
    }

    /** Updates the caller's own contact details, refusing an email or phone now used by a different account. */
    @Transactional
    public UserResponse updateCurrentUser(Long userId, UserRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email already exists");
        }
        if (!user.getPhone().equals(phone) && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone already exists");
        }

        user.setName(request.getName());
        user.setEmail(email);
        user.setPhone(phone);

        return mapToResponse(userRepository.save(user));
    }

    /** Addresses are stored lowercased and trimmed so one account is one address. */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    /**
     * The registration screen offers the 47 Kenyan counties, so a
     * region that is not one of them is refused rather than stored
     * in a spelling the screen will never send again.
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

    /** Sends reset instructions without disclosing account existence. */
    @Transactional
    public String forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(user -> {
            if (passwordResetFrontendUrl == null || passwordResetFrontendUrl.isBlank()) return;
            String raw = newResetToken();
            passwordResetTokenRepository.deleteByUserId(user.getId());
            PasswordResetToken token = PasswordResetToken.builder().userId(user.getId()).tokenHash(hash(raw))
                    .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpiryMinutes)).createdAt(LocalDateTime.now()).build();
            passwordResetTokenRepository.save(token);
            String joiner = passwordResetFrontendUrl.contains("?") ? "&" : "?";
            boolean sent = sendGridMailService.sendHtml(user.getEmail(), "Reset your Kilivana password",
                    "<p>Use this link within " + passwordResetExpiryMinutes + " minutes: <a href=\"" + passwordResetFrontendUrl + joiner + "token=" + raw + "\">Reset password</a></p>");
            if (!sent) passwordResetTokenRepository.delete(token);
        });
        return "If the account exists and email delivery is available, reset instructions will be sent.";
    }

    @Transactional
    public String resetPassword(PasswordResetRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashForUpdate(hash(request.getToken()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())) throw new BadRequestException("Invalid or expired password reset token");
        User user = userRepository.findById(token.getUserId()).orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        token.setUsedAt(LocalDateTime.now());
        return "Password has been reset successfully.";
    }

    private static String newResetToken() { byte[] bytes = new byte[32]; RESET_RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String hash(String value) { try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }

    /**
     * Issues a new token pair from a refresh token. Refuses tokens of the wrong type,
     * unknown users, and accounts that are suspended or inactive.
     */
    public AuthTokenResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenType(refreshToken, JwtService.TYPE_REFRESH)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedException("This account is " + user.getStatus().name().toLowerCase());
        }

        return buildTokenResponse(user);
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
