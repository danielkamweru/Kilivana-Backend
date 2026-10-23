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
import com.kilivana.backend.common.exception.TooManyRequestsException;
import com.kilivana.backend.common.exception.UnauthorizedException;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.service.KenyaCounty;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Self-service registration, login, token refresh and password reset.
 *
 * <p>Staff roles (ADMIN, INSPECTOR) cannot self-register; they are created by an existing
 * administrator through the admin panel. Passwords are never returned; only JWT access and
 * refresh tokens are issued. The access token expiry is configurable; a non-positive value
 * means the token carries no expiry and stays valid until the signing secret changes.
 */
@Service
@Slf4j
    @RequiredArgsConstructor
    public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserReferenceCodeGenerator referenceCodeGenerator;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SendGridMailService sendGridMailService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.password-reset.frontend-url:}")
    private String passwordResetFrontendUrl;
    /**
     * Recovery link for the mobile app (drivers). A deep link the app claims, such as
     * {@code kilivana://reset-password}, or an app link the app intercepts.
     */
    @Value("${app.password-reset.client-urls.mobile:}")
    private String mobileResetUrl = "";
    /** Recovery link for the ecommerce storefront (buyers). */
    @Value("${app.password-reset.client-urls.ecommerce:}")
    private String ecommerceResetUrl = "";
    /** Recovery link for the farmer portal. */
    @Value("${app.password-reset.client-urls.farmer:}")
    private String farmerResetUrl = "";
    /** Recovery link for the supplier portal. */
    @Value("${app.password-reset.client-urls.supplier:}")
    private String supplierResetUrl = "";
    /** Recovery link for the inspector portal. */
    @Value("${app.password-reset.client-urls.inspector:}")
    private String inspectorResetUrl = "";
    /** Recovery link for the admin dashboard. */
    @Value("${app.password-reset.client-urls.admin:}")
    private String adminResetUrl = "";
    @Value("${app.password-reset.token-expiry-minutes:30}")
    private long passwordResetExpiryMinutes = 30L;
    /**
     * Minimum gap between two recovery requests for the same address. Recovery mail is
     * the expensive part of the flow, and every new request also replaces the previous
     * token, so an unthrottled caller can flood a mailbox and invalidate a link that
     * the owner is still reading.
     */
    @Value("${app.password-reset.request-cooldown-minutes:2}")
    private long passwordResetCooldownMinutes = 2L;
    /** Normalized address -> instant of the last accepted recovery request. */
    private final Map<String, Instant> resetRequestCooldowns = new ConcurrentHashMap<>();
    /**
     * Overrides the configured reset-link base URL. Package-private so the test
     * suite can drive the reset flow without a real deployment value.
     */
    void setPasswordResetFrontendUrl(String url) {
        this.passwordResetFrontendUrl = url;
    }

    /**
     * Overrides the recovery link for the mobile app. Package-private so the test
     * suite can drive the reset flow without a real deployment value.
     */
    void setMobileResetUrl(String url) {
        this.mobileResetUrl = url;
    }

    /**
     * Overrides the recovery link for the ecommerce storefront. Package-private so the
     * test suite can drive the reset flow without a real deployment value.
     */
    void setEcommerceResetUrl(String url) {
        this.ecommerceResetUrl = url;
    }

    /** Overrides the recovery link for the farmer portal. */
    void setFarmerResetUrl(String url) {
        this.farmerResetUrl = url;
    }

    /** Overrides the recovery link for the supplier portal. */
    void setSupplierResetUrl(String url) {
        this.supplierResetUrl = url;
    }

    /** Overrides the recovery link for the inspector portal. */
    void setInspectorResetUrl(String url) {
        this.inspectorResetUrl = url;
    }

    /** Overrides the recovery link for the admin dashboard. */
    void setAdminResetUrl(String url) {
        this.adminResetUrl = url;
    }

    /**
     * Overrides the configured recovery cooldown. Package-private so the test suite can
     * drive the reset flow without waiting for a real cooldown to elapse.
     */
    void setPasswordResetCooldownMinutes(long minutes) {
        this.passwordResetCooldownMinutes = minutes;
    }


    private static final SecureRandom RESET_RANDOM = new SecureRandom();

    /**
     * Self-service registration. Refuses staff roles, rejects an email or phone already
     * taken, and normalizes the email, phone and county before the row is written.
     */
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        if (request.getRole() == null || request.getRole() == UserRole.ADMIN || request.getRole() == UserRole.INSPECTOR) {
            throw new BadRequestException(
                    "Staff accounts require authorized administrator provisioning");
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
                .emailVerified(false)
                .phone(phone)
                .username(request.getUsername())
                .region(normaliseRegion(request.getRegion()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .referenceCode(referenceCodeGenerator.nextCode(request.getRole()))
                .build();

        User saved = userRepository.save(user);
        var challenge = emailVerificationService.startForNewAccount(saved);
        UserResponse response = mapToResponse(saved);
        response.setEmailVerificationChallengeId(challenge.challengeId());
        return response;
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

        if (!user.isEmailVerified()) {
            var challenge = emailVerificationService.request(user.getEmail());
            return AuthTokenResponse.builder()
                    .emailVerificationRequired(true)
                    .emailVerificationChallengeId(challenge.challengeId())
                    .user(mapToResponse(user))
                    .build();
        }
        return buildTokenResponse(user);
    }

    private AuthTokenResponse buildTokenResponse(User user) {
        if (!user.isEmailVerified())
            throw new com.kilivana.backend.common.exception.ForbiddenException("Email verification is required before login");
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
        User user = userRepository.findByIdForUpdate(userId)
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

    public String forgotPassword(String email) {
        return forgotPassword(email, null);
    }

    /** Sends reset instructions without disclosing account existence. */
    @Transactional
    public String forgotPassword(String email, String application) {
        // Validate the application identifier up front, before any user lookup, so an
        // unknown identifier is rejected without disclosing whether the email exists.
        if (application != null && !application.isBlank()) {
            String app = application.trim().toLowerCase(java.util.Locale.ROOT);
            if (!ALLOWED_RESET_APPLICATIONS.contains(app)) {
                throw new BadRequestException("Unknown password-reset application: " + application);
            }
        }
        String address = normalizeEmail(email);
        if (!acceptRecoveryRequest(address)) {
            throw new TooManyRequestsException(
                    "A password reset was requested recently. Please wait before asking again.");
        }
        userRepository.findByEmailIgnoreCase(address).ifPresent(user -> {
            String resetUrl = resolveResetUrl(application, user.getRole());
            if (resetUrl == null || resetUrl.isBlank()) return;
            String raw = newResetToken();
            passwordResetTokenRepository.deleteByUserId(user.getId());
            PasswordResetToken token = PasswordResetToken.builder().userId(user.getId()).tokenHash(hash(raw))
                    .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpiryMinutes)).createdAt(LocalDateTime.now()).build();
            passwordResetTokenRepository.save(token);
            String resetLink = appendResetToken(resetUrl, raw);
            boolean sent = sendGridMailService.sendHtml(user.getEmail(), "Reset your Kilivana password",
                    resetMailBody(resetLink));
            if (!sent) {
                // Nothing left the building, so the throttle has no flood to prevent:
                // the caller may try again at once. A cooldown that survived a refused
                // delivery would only make an unworking mailer look like a broken
                // request.
                resetRequestCooldowns.remove(address);
                passwordResetTokenRepository.delete(token);
            }
        });
        return "If the account exists and email delivery is available, reset instructions will be sent.";
    }

    /**
     * Accepts a recovery request unless the same address asked recently. The cooldown is
     * keyed by address alone, so it applies to unknown addresses exactly as it applies to
     * registered ones and cannot be used to probe for an account.
     */
    private boolean acceptRecoveryRequest(String address) {
        if (passwordResetCooldownMinutes <= 0) {
            return true;
        }
        Instant now = Instant.now();
        Instant previous = resetRequestCooldowns.put(address, now);
        if (previous == null) {
            return true;
        }
        Instant cutoff = now.minus(Duration.ofMinutes(passwordResetCooldownMinutes));
        if (previous.isAfter(cutoff)) {
            resetRequestCooldowns.put(address, previous);
            return false;
        }
        return true;
    }

    /**
     * Which client a recovery link points at. Every account role has its own configured
     * destination, with the generic web page as its only fallback. The account role
     * decides where the user is going to sign in again.
     */
    private static final Map<UserRole, String> ROLE_CLIENTS = Map.of(
            UserRole.DRIVER, "mobile",
            UserRole.BUYER, "ecommerce",
            UserRole.FARMER, "farmer",
            UserRole.SUPPLIER, "supplier",
            UserRole.INSPECTOR, "inspector",
            UserRole.ADMIN, "admin");

    /**
     * The recovery link base URL for a user's client. A client without a configured URL
     * falls back to the web link, so an unconfigured mobile app never leaves the user
     * without a way in. An empty result means no link can be built and no mail is sent.
     */
    private static final java.util.Set<String> ALLOWED_RESET_APPLICATIONS =
            java.util.Set.of("driver", "ecommerce", "farmer", "supplier", "inspector", "admin", "web",
                    "mobile", "buyer");

    /**
     * Resolves the recovery-link base URL for the requested application, falling back
     * to the user's role when no application is supplied. An unknown application
     * identifier is rejected rather than silently ignored.
     */
    private String resolveResetUrl(String application, UserRole role) {
        String client = application == null || application.isBlank()
                ? (role == null ? "web" : ROLE_CLIENTS.getOrDefault(role, "web"))
                : application.trim().toLowerCase(java.util.Locale.ROOT);
        if (!ALLOWED_RESET_APPLICATIONS.contains(client)) {
            throw new BadRequestException("Unknown password-reset application: " + application);
        }
        String preferredUrl = switch (client) {
            case "mobile", "driver" -> mobileResetUrl;
            case "ecommerce", "buyer" -> ecommerceResetUrl;
            case "farmer" -> farmerResetUrl;
            case "supplier" -> supplierResetUrl;
            case "inspector" -> inspectorResetUrl;
            case "admin" -> adminResetUrl;
            default -> passwordResetFrontendUrl;
        };
        return firstUsableResetUrl(preferredUrl, passwordResetFrontendUrl);
    }

    private String firstUsableResetUrl(String... candidates) {
        for (String candidate : candidates) {
            if (isSafeResetUrl(candidate)) {
                return candidate.trim();
            }
        }
        log.error("No valid password-reset destination is configured; recovery email was not sent");
        return "";
    }

    private static boolean isSafeResetUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null && uri.getFragment() == null;
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    private static String appendResetToken(String resetUrl, String rawToken) {
        return UriComponentsBuilder.fromUriString(resetUrl)
                .replaceQueryParam("token", rawToken)
                .build().encode().toUriString();
    }

    /**
     * The body of the recovery mail. A deep link opens the app, a web link opens the
     * browser, and the raw link is always included so a mail client that does not
     * follow links still leaves the user a way in.
     */
    private String resetMailBody(String resetLink) {
        boolean opensApp = !resetLink.startsWith("http://") && !resetLink.startsWith("https://");
        String instruction = opensApp
                ? "Tap the button below to reset your password in the Kilivana app."
                : "Tap the button below to reset your password.";
        return "<p>Hi,</p>"
                + "<p>" + instruction + "</p>"
                + "<p><a href=\"" + resetLink + "\">Reset password</a></p>"
                + "<p>If the button does not work, paste this link into your browser:<br>"
                + resetLink + "</p>"
                + "<p>This link can be used once and expires in " + passwordResetExpiryMinutes + " minutes.</p>"
                + "<p>If you did not ask to reset your password, you can ignore this email "
                + "and nothing will change.</p>";
    }

    @Transactional
    public String resetPassword(PasswordResetRequest request) {
        PasswordPolicy.requireAcceptable(request.getNewPassword());
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
                .emailVerified(user.isEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
