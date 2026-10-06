package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AdminBuyerResponse;
import com.kilivana.backend.admin.service.AdminBuyerService;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "List buyers",
            description = "Every buyer account with its profile, order count, total spend and dispute count.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBuyerResponse>>> listBuyers() {
        return ResponseEntity.ok(ApiResponse.success(adminBuyerService.listBuyers()));
    }
}
