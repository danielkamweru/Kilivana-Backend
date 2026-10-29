package com.kilivana.backend.common.controller;

import com.kilivana.backend.common.dto.*;
import com.kilivana.backend.common.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="Authentication", description="Registration, login, token refresh, email verification and account recovery")
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/auth/email-verification")
public class EmailVerificationController {
    private final EmailVerificationService verification;
    @PostMapping("/request")
    @Operation(summary="Request or resend an email verification code",description="Generic acknowledgment; repeated requests during the 60-second cooldown reuse the active challenge without sending another email. Maximum five sends per email per hour. Does not disclose account existence.")
    public ResponseEntity<ApiResponse<EmailOtpChallengeResponse>> request(@Valid @RequestBody EmailOtpRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success(verification.request(request.getEmail())));
    }
    @PostMapping("/verify")
    @Operation(summary="Verify email using a single-use six-digit code",description="Confirms email only; never changes account role, status or administrator approval. Does not issue JWTs.")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody EmailOtpVerifyRequest request) {
        verification.verify(request);
        return ResponseEntity.ok(ApiResponse.successMessage("Email verified successfully. Continue to login and any required approval steps."));
    }
}
