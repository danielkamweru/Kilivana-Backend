package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.UserProfileUpdateRequest;
import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.success(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateOwnProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UserRegistrationRequest serviceRequest = UserRegistrationRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userId, serviceRequest)));
    }
}