package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AdminBuyerResponse;
import com.kilivana.backend.admin.service.AdminBuyerService;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The buyer roster as an administrator sees it: account, profile and order/dispute
 * summary in one read. A buyer without a profile is still listed, with their profile
 * fields null, because a registered buyer who has not onboarded yet is still a buyer
 * the panel has to show.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/buyers")
@RequiredArgsConstructor
public class AdminBuyerController {

    private final AdminBuyerService adminBuyerService;
    private final UserService userService;

    @Operation(summary = "List buyers",
            description = "Every buyer account with its profile, order count, total spend and dispute count.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBuyerResponse>>> listBuyers() {
        return ResponseEntity.ok(ApiResponse.success(adminBuyerService.listBuyers()));
    }

    @Operation(summary = "Suspend a buyer",
            description = "Suspends the buyer account. The account stays; the buyer cannot place orders.")
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendBuyer(
            @PathVariable Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(userService.suspendUser(userId, reason)));
    }

    @Operation(summary = "Unsuspend a buyer",
            description = "Reinstates a suspended buyer account.")
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<UserResponse>> unsuspendBuyer(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.activateUser(userId)));
    }
}
