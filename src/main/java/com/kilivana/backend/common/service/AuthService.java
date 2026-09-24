package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.AuthLoginRequest;
import com.kilivana.backend.common.dto.AuthTokenResponse;
import com.kilivana.backend.common.dto.PasswordResetRequest;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    public AuthTokenResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return AuthTokenResponse.builder()
                .accessToken("demo-access-token-" + user.getId())
                .refreshToken("demo-refresh-token-" + user.getId())
                .tokenType("Bearer")
                .user(mapToResponse(user))
                .build();
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(Long userId, UserRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        if (!user.getPhone().equals(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone already exists");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public String forgotPassword(String email) {
        userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User with email not found"));
        return "Password reset instructions have been sent if this account exists.";
    }

    @Transactional
    public String resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password reset successfully";
    }

    public AuthTokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !refreshToken.startsWith("demo-refresh-token-")) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = Long.parseLong(refreshToken.replace("demo-refresh-token-", ""));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return AuthTokenResponse.builder()
                .accessToken("demo-access-token-" + user.getId())
                .refreshToken("demo-refresh-token-" + user.getId())
                .tokenType("Bearer")
                .user(mapToResponse(user))
                .build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .verificationStatus(user.getVerificationStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
