package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.logistics.dto.BuyerDeliverySummaryResponse;
import com.kilivana.backend.logistics.service.LogisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Buyer delivery", description = "Buyer-owned delivery summaries")
@RestController
@RequestMapping("/api/v1/buyer/orders")
@RequiredArgsConstructor
public class BuyerDeliveryController {

    private final LogisticsService logisticsService;

    @Operation(summary = "Get delivery status for a buyer-owned order")
    @GetMapping("/{orderId}/delivery")
    public ResponseEntity<ApiResponse<BuyerDeliverySummaryResponse>> getDeliverySummary(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                logisticsService.getBuyerDeliverySummary(orderId)));
    }

}
