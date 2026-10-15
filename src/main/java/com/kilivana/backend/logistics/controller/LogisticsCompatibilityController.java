package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.entity.TrackingEvent;
import com.kilivana.backend.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Compatibility endpoints mounted on the delivery job path: record a
 * tracking event, read a job's tracking history, or file proof of
 * delivery for a given job.
 *
 * <p>These exist alongside the dedicated controllers ({@code TrackingEventController},
 * {@code ProofOfDeliveryController}) because some clients address resources by job id in
 * the path. They delegate to the same {@code LogisticsService} methods, so there is one
 * implementation of the business rules and two ways to reach it.
 */
@Tag(name = "Logistics · Delivery Jobs", description = "Delivery job creation, driver assignment and status")
@RestController
@RequestMapping("/api/v1/logistics/jobs")
@RequiredArgsConstructor
public class LogisticsCompatibilityController {

    private final LogisticsService logisticsService;

    @Operation(summary = "Get the tracking history of a job",
            description = "Compatibility variant of GET /api/v1/logistics/tracking-events/job/{jobId}.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking history returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{jobId}/tracking")
    public ResponseEntity<ApiResponse<List<TrackingEvent>>> getTracking(
            @Parameter(description = "ID of the delivery job") @PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(logisticsService.getTrackingHistory(jobId)));
    }

    @Operation(summary = "File proof of delivery for a job",
            description = "Compatibility variant of POST /api/v1/logistics/proof-of-delivery that addresses the delivery job by path.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Proof of delivery filed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Delivery code missing, expired or incorrect, or malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This delivery has already been confirmed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many incorrect delivery codes; the code is temporarily locked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{jobId}/proof-of-delivery")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> submitProof(
            @Parameter(description = "ID of the delivery job") @PathVariable Long jobId,
            @RequestBody ProofOfDelivery proof,
            @Parameter(description = "Six-digit delivery handover code sent to the buyer; required while the job still has an unverified delivery code", example = "123456") @RequestParam(required = false) String otp) {
        proof.setLogisticsJobId(jobId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(logisticsService.createProofOfDelivery(proof, otp)));
    }
}
