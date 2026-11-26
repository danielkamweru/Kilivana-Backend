package com.kilivana.backend.finance.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.finance.service.MpesaB2CService;
import com.kilivana.backend.finance.service.SellerPayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * M-Pesa B2C callback endpoints for seller payouts.
 * These endpoints are public (no auth) as they are called by Safaricom.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/b2c")
@RequiredArgsConstructor
public class B2CCallbackController {

    private final MpesaB2CService b2cService;
    private final SellerPayoutService payoutService;

    /**
     * B2C Result callback - called when B2C payment completes (success or failure).
     */
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Map<String, String>>> b2cResultCallback(@RequestBody Map<String, Object> callback) {
        log.info("Received B2C result callback");

        try {
            b2cService.processB2CCallback(callback);
            // The actual payout processing is done asynchronously or via the stored result
            // We acknowledge immediately to stop Safaricom retries
            return ResponseEntity.ok(ApiResponse.success(Map.of("ResultCode", "0", "ResultDesc", "Accepted")));
        } catch (Exception e) {
            log.error("Error processing B2C callback: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Map.of("ResultCode", "0", "ResultDesc", "Accepted")));
        }
    }

    /**
     * B2C Queue timeout callback - called when B2C request times out in Safaricom queue.
     */
    @PostMapping("/queue-timeout")
    public ResponseEntity<ApiResponse<Map<String, String>>> b2cQueueTimeoutCallback(@RequestBody Map<String, Object> callback) {
        log.info("Received B2C queue timeout callback");

        try {
            b2cService.processB2CQueueCallback(callback);
            return ResponseEntity.ok(ApiResponse.success(Map.of("ResultCode", "0", "ResultDesc", "Accepted")));
        } catch (Exception e) {
            log.error("Error processing B2C queue callback: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Map.of("ResultCode", "0", "ResultDesc", "Accepted")));
        }
    }
}