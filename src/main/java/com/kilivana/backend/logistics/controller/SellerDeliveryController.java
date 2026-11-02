package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.logistics.dto.BuyerDeliverySummaryResponse;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Seller delivery", description = "Seller-scoped delivery visibility")
@RestController
@RequestMapping("/api/v1/seller/orders")
@RequiredArgsConstructor
public class SellerDeliveryController {

    private final LogisticsService logisticsService;

    @Operation(summary = "Get this seller's deliveries for an order")
    @GetMapping("/{orderId}/deliveries")
    public ResponseEntity<ApiResponse<BuyerDeliverySummaryResponse>> getSellerDeliverySummary(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                logisticsService.getSellerDeliverySummary(orderId, CurrentUser.id())));
    }
}
