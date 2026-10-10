package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.PaymentRequest;
import com.kilivana.backend.ecommerce.dto.PaymentResponse;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.service.PaymentService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing payments.
 * Supports initiation, retrieval, status transitions and refunds.
 * Payments are linked to orders and track payment method, provider reference and amount.
 */
@Tag(name = "E-Commerce · Payments", description = "Payment initiation, webhooks, refunds and status")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a new payment record from the supplied request payload.
     * The payment method can be provided via either the {@code method} or {@code provider}
     * field for backward compatibility.
     *
     * @param request the payment details (order ID, method, reference, amount)
     * @return the created payment
     */
    @Operation(summary = "Create payment", description = "Creates a new payment record from the supplied request payload and returns the created payment.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Parameter(description = "Payment creation details") @RequestBody PaymentRequest request) {
        Payment payment = paymentService.createPayment(request.getOrderId(), request.resolveMethod(),
                request.getReference(), request.getAmount());
        PaymentResponse response = paymentService.mapToResponse(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Initiates a payment by delegating to the create-payment workflow.
     * Alternative endpoint name for clients expecting an "initiate" action.
     *
     * @param request the payment details
     * @return the created payment
     */
    @Operation(summary = "Initiate payment", description = "Initiates a payment by delegating to the create-payment workflow.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment initiated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(@Parameter(description = "Payment initiation details") @RequestBody PaymentRequest request) {
        return createPayment(request);
    }

    /**
     * Receives an asynchronous payment notification (webhook) and processes it
     * as a new payment record. Accepts the same payload as the create endpoint.
     *
     * @param request the webhook notification payload
     * @return the created payment
     */
    @Operation(summary = "Payment webhook", description = "Receives an asynchronous payment notification and processes it as a new payment record.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Webhook processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid webhook payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<PaymentResponse>> paymentWebhook(@Parameter(description = "Webhook notification payload") @RequestBody PaymentRequest request) {
        return createPayment(request);
    }

    /**
     * Retrieves a single payment by its identifier.
     *
     * @param id the payment identifier
     * @return the payment
     */
    @Operation(summary = "Get payment by id", description = "Retrieves a single payment by its identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@Parameter(description = "Payment identifier") @PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = paymentService.mapToResponse(payment);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves all payments associated with a given order.
     *
     * @param orderId the order identifier
     * @return list of payments for that order
     */
    @Operation(summary = "Get payments by order", description = "Retrieves all payments associated with a given order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByOrder(@Parameter(description = "Order identifier") @PathVariable Long orderId) {
        List<Payment> payments = paymentService.getPaymentsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(
                payments.stream().map(paymentService::mapToResponse).toList()));
    }

    /**
     * Transitions a payment to a new status.
     * Valid statuses: PENDING, COMPLETED, FAILED, REFUNDED, HELD.
     *
     * @param id the payment identifier
     * @param status the new payment status
     * @return the updated payment
     */
    @Operation(summary = "Update payment status", description = "Transitions a payment to a new status and returns the updated payment.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or unknown status value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @Parameter(description = "Payment identifier") @PathVariable Long id,
            @Parameter(description = "New payment status value") @RequestParam String status) {
        Payment payment = paymentService.updatePaymentStatus(id, PaymentStatus.from(status));
        PaymentResponse response = paymentService.mapToResponse(payment);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refunds a payment by setting its status to REFUNDED.
     * This is a convenience endpoint that delegates to the status update workflow.
     *
     * @param id the payment identifier
     * @return the refunded payment
     */
    @Operation(summary = "Refund payment", description = "Refunds a payment by setting its status to REFUNDED and returns the updated payment.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment refunded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@Parameter(description = "Payment identifier") @PathVariable Long id) {
        Payment payment = paymentService.updatePaymentStatus(id, PaymentStatus.REFUNDED);
        return ResponseEntity.ok(ApiResponse.success(paymentService.mapToResponse(payment)));
    }
}
