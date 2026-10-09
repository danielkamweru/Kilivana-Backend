package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.UserProfileUpdateRequest;
import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Account self-service and address book management. This is the only entry point a user uses
 * to register themselves or update their own profile; the heavier administration endpoints
 * live on the admin controllers. The two base paths — <code>/api/v1/users</code> and
 * <code>/api/users</code> — are kept in sync so older clients keep working.
 */
@Tag(name = "Administration · Users & Addresses", description = "Account self-service and address book management")
@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Registers a new user account. Public: no token is required.
     *
     * @param request the registration payload, including name, email, phone, password and role
     * @return the created user
     */
    @Operation(
        summary = "Register a user",
        description = "Creates a new user account from the registration payload."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or email already registered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email, phone or username already registered")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.success(user));
    }

    /**
     * Fetches a user's own profile by id.
     *
     * @param id the user id
     * @return the user, or 404 if no such account exists
     */
    @Operation(
        summary = "Get a user profile",
        description = "Returns a single user by their id."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user with that id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    /**
     * Updates the authenticated caller's own profile. Only name, email, phone and region are
     * accepted here; password changes go through the dedicated auth flow.
     *
     * @param userId   the authenticated caller's id, supplied by the JWT filter
     * @param request  the fields to update
     * @return the updated user
     */
    @Operation(
        summary = "Update my profile",
        description = "Updates the authenticated caller's name, email, phone and region. "
                + "Password changes are not accepted here."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone already registered")
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateOwnProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UserRegistrationRequest serviceRequest = UserRegistrationRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .region(request.getRegion())
                .build();
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userId, serviceRequest)));
    }
}