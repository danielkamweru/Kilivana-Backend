package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.DisputeRequest;
import com.kilivana.backend.ecommerce.dto.DisputeResolutionRequest;
import com.kilivana.backend.ecommerce.dto.DisputeResponse;
import com.kilivana.backend.ecommerce.service.DisputeService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.DisputeStatus;
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
 * REST controller for managing order disputes.
 * Supports creation, retrieval, status transitions and resolution.
 * Disputes are associated with orders and can be raised by users.
 */
@Tag(name = "E-Commerce · Orders", description = "Order placement, status transitions, timeline and disputes")
@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    /**
     * Creates a new dispute from the supplied request payload.
     *
     * @param request the dispute details (order ID, raised by user ID, reason, description, initial status)
     * @return the created dispute
     */
    @Operation(summary = "Create dispute", description = "Creates a new dispute from the supplied request payload and returns the created dispute.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Dispute created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Referenced order not found")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(@Parameter(description = "Dispute creation details") @RequestBody DisputeRequest request) {
        DisputeResponse dispute = disputeService.createDispute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dispute));
    }

    /**
     * Retrieves a single dispute by its identifier.
     *
     * @param id the dispute identifier
     * @return the dispute
     */
    @Operation(summary = "Get dispute by id", description = "Retrieves a single dispute by its identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispute found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDisputeById(@Parameter(description = "Dispute identifier") @PathVariable Long id) {
        DisputeResponse dispute = disputeService.getDisputeById(id);
        return ResponseEntity.ok(ApiResponse.success(dispute));
    }

    /**
     * Retrieves all disputes associated with a specific order.
     *
     * @param orderId the order identifier
     * @return list of disputes for that order
     */
    @Operation(summary = "Get disputes by order", description = "Retrieves all disputes associated with a specific order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Disputes retrieved successfully")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputesByOrder(@Parameter(description = "Order identifier") @PathVariable Long orderId) {
        List<DisputeResponse> disputes = disputeService.getDisputesByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    /**
     * Retrieves all disputes associated with a specific user.
     *
     * @param userId the user identifier
     * @return list of disputes for that user
     */
    @Operation(summary = "Get disputes by user", description = "Retrieves all disputes associated with a specific user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Disputes retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputesByUser(@Parameter(description = "User identifier") @PathVariable Long userId) {
        List<DisputeResponse> disputes = disputeService.getDisputesByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    /**
     * Transitions a dispute to a new status.
     *
     * @param id the dispute identifier
     * @param status the new dispute status (e.g., OPEN, IN_REVIEW, RESOLVED)
     * @return the updated dispute
     */
    @Operation(summary = "Update dispute status", description = "Transitions a dispute to a new status and returns the updated dispute.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispute status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or unknown status value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DisputeResponse>> updateDisputeStatus(
            @Parameter(description = "Dispute identifier") @PathVariable Long id,
            @Parameter(description = "New dispute status") @RequestParam DisputeStatus status) {
        DisputeResponse dispute = disputeService.updateDisputeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(dispute));
    }

    /**
     * Resolves a dispute with the provided resolution details.
     * The outcome determines whether the order is cancelled/refunded (BUYER)
     * or completed with payment released to the seller (FARMER).
     *
     * @param id the dispute identifier
     * @param request the resolution outcome and explanation
     * @return the resolved dispute
     */
    @Operation(summary = "Resolve dispute", description = "Resolves a dispute with the provided resolution details and returns the updated dispute.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispute resolved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    @PutMapping("/{id}/resolution")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolveDispute(
            @Parameter(description = "Dispute identifier") @PathVariable Long id,
            @Parameter(description = "Dispute resolution details") @RequestBody DisputeResolutionRequest request) {
        DisputeResponse dispute = disputeService.resolveDispute(id, request);
        return ResponseEntity.ok(ApiResponse.success(dispute));
    }
}
