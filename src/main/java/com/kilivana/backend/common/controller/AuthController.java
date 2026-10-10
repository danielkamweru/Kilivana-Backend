package com.kilivana.backend.common.controller;

import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.dto.AuthLoginRequest;
import com.kilivana.backend.common.dto.AuthTokenResponse;
import com.kilivana.backend.common.dto.PasswordResetRequest;
import com.kilivana.backend.common.dto.RefreshTokenRequest;
import com.kilivana.backend.common.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.kilivana.backend.security.CurrentUser;

/**
 * Authentication endpoints: registration, login, token refresh and password recovery.
 *
 * <p>Mapped under both {@code /api/v1/auth} and {@code /api/auth} so clients written
 * against the older, unprefixed path keep working.
 */
@Tag(name = "Authentication", description = "Registration, login, token refresh and account recovery")
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Creates an account. Staff roles are refused here; an administrator creates staff accounts through the admin panel. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    /** Authenticates with email and password and returns a fresh access/refresh token pair plus the user's profile. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthTokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    /**
     * Reports a successful logout. Tokens are stateless JWTs, so the server holds no
     * session to invalidate; the client simply discards them.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.successMessage("Logout successful"));
    }

    /** Exchanges a valid refresh token for a new access/refresh pair. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        AuthTokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Sends reset instructions when the account exists. The response is identical either
     * way, so registered emails cannot be enumerated through this endpoint.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        String message = authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /** Sets a new password for the account the given email belongs to. */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /** Returns the profile of the user the bearer token identifies. */
    @GetMapping("/me")
    @Operation(
            summary = "Get the authenticated user",
            description = "Returns the profile of the user identified by the bearer token."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser(CurrentUser.id())));
    }
}
