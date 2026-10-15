package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.dto.DarajaCallbackRequest;
import com.kilivana.backend.dto.DarajaCallbackResponse;
import com.kilivana.backend.service.DarajaCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Callback endpoint for Safaricom Daraja (M-PESA STK Push) results.
 *
 * <p>This endpoint is publicly accessible (no JWT required) because Safaricom
 * cannot provide authentication headers. Security is enforced through:
 * <ul>
 *   <li>Matching the callback to a pending STK request by CheckoutRequestID</li>
 *   <li>Validating the MerchantRequestID against the stored record</li>
 *   <li>Idempotent processing — duplicate callbacks are ignored</li>
 *   <li>Rate limiting on the callback path (configured at the infrastructure level)</li>
 * </ul>
 *
 * <p>The callback URL must be a public HTTPS endpoint (use ngrok for local
 * testing). Configure it via {@code DARAJA_CALLBACK_URL} environment variable.
 */
@Tag(name = "Payments · Daraja Callback",
        description = "Public callback endpoint for Safaricom M-PESA STK Push results")
@RestController
@RequestMapping("/api/v1/payments/daraja")
@RequiredArgsConstructor
public class DarajaCallbackController {

    private final DarajaCallbackService callbackService;

    /**
     * Receives the STK Push result from Safaricom.
     *
     * <p>Called by Safaricom after the buyer completes (or cancels) the STK
     * prompt on their phone. The callback is processed idempotently — repeated
     * deliveries with the same CheckoutRequestID are safely ignored after the
     * first successful processing.
     *
     * @param callback  the Daraja callback payload
     * @param request   HTTP request (for logging client IP)
     * @return acknowledgment to Safaricom (must return 200 OK within 30s)
     */
    @Operation(summary = "Receive M-PESA STK Push callback",
            description = "Public callback endpoint for Safaricom to post STK Push results. " +
                    "Idempotent: duplicate callbacks for the same CheckoutRequestID are safely ignored. " +
                    "Must return 200 OK within 30 seconds to avoid Safaricom retries.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Callback acknowledged (even if processing fails internally)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed callback payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal error (Safaricom will retry)")
    })
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<DarajaCallbackResponse>> callback(
            @RequestBody DarajaCallbackRequest callback,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        logCallback("Received Daraja callback from {}: MerchantRequestID={}, CheckoutRequestID={}, ResultCode={}",
                clientIp,
                callback.getBody() != null ? callback.getBody().getMerchantRequestID() : "null",
                callback.getBody() != null ? callback.getBody().getCheckoutRequestID() : "null",
                callback.getBody() != null ? callback.getBody().getResultCode() : "null");

        DarajaCallbackResponse response = callbackService.processCallback(callback, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private static void logCallback(String format, Object... args) {
        // Intentionally minimal logging — no sensitive callback data in logs
        System.out.println("[DARAJA_CALLBACK] " + String.format(format, args));
    }
}