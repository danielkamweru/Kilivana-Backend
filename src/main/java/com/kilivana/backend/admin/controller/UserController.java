package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin controller for user management operations.
 * Provides endpoints for creating, reading, updating, and deleting users,
 * as well as filtering by role and status, and suspending/activating accounts.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user", description = "Registers a new user account with the provided details. Returns the created user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody UserRegistrationRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a single user by their unique identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "List all users", description = "Returns a paginated list of all users in the system.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(Pageable pageable) {
        List<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @Operation(summary = "Get users by role", description = "Returns all users having the specified role.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid role value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(
            @Parameter(description = "User role to filter by", example = "ADMIN") @PathVariable UserRole role) {
        List<UserResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @Operation(summary = "Get users by status", description = "Returns all users having the specified status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByStatus(
            @Parameter(description = "User status to filter by", example = "ACTIVE") @PathVariable UserStatus status) {
        List<UserResponse> users = userService.getUsersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @Operation(summary = "Update user", description = "Updates an existing user's details. Requires full user data in request body.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already in use by another user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id,
            @RequestBody UserRegistrationRequest request) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "Update user status (PUT)", description = "Changes the status of a user (e.g., ACTIVE, SUSPENDED, INACTIVE).")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id,
            @Parameter(description = "New status to assign", example = "SUSPENDED") @RequestParam UserStatus status) {
        UserResponse user = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "Update user status (PATCH)", description = "Partially updates the status of a user. Alias for PUT /{id}/status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> patchUserStatus(
            @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id,
            @Parameter(description = "New status to assign", example = "SUSPENDED") @RequestParam UserStatus status) {
        return updateUserStatus(id, status);
    }

    @Operation(summary = "Delete user", description = "Permanently deletes a user from the system.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.successMessage("User deleted successfully"));
    }

    @Operation(summary = "Suspend user", description = "Suspends a user account with a mandatory reason.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User suspended successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Reason is required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendUser(
            @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id,
            @Parameter(description = "Reason for suspension", example = "Violation of terms of service") @RequestParam String reason) {
        UserResponse user = userService.suspendUser(id, reason);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "Activate user", description = "Activates a previously suspended or inactive user account.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User activated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @Parameter(description = "Unique identifier of the user", example = "1") @PathVariable Long id) {
        UserResponse user = userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
