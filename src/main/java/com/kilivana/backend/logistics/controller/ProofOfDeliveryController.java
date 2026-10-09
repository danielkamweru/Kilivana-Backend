package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Proof of delivery: capturing the recipient, signature and photo evidence
 * that a delivery was handed over, and retrieving or deleting that evidence.
 */
@Tag(name = "Logistics · Proof of Delivery", description = "Proof of delivery capture")
@RestController
@RequestMapping("/api/v1/logistics/proof-of-delivery")
@RequiredArgsConstructor
public class ProofOfDeliveryController {

    private final LogisticsService logisticsService;

    @Operation(summary = "File proof of delivery",
            description = "Files proof of delivery for a delivery job. A job whose delivery code has not been verified yet must supply the correct code.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Proof of delivery filed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Delivery code missing, expired or incorrect, or malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This delivery has already been confirmed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many incorrect delivery codes; the code is temporarily locked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProofOfDelivery>> createProofOfDelivery(
            @RequestBody ProofOfDelivery proof,
            @Parameter(description = "Six-digit delivery handover code sent to the buyer; required while the job still has an unverified delivery code", example = "123456") @RequestParam(required = false) String otp) {
        ProofOfDelivery created = logisticsService.createProofOfDelivery(proof, otp);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "Get proof of delivery by ID", description = "Returns a single proof of delivery.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proof of delivery found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proof of delivery not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> getProofById(
            @Parameter(description = "ID of the proof of delivery") @PathVariable Long id) {
        ProofOfDelivery proof = logisticsService.getProofById(id);
        return ResponseEntity.ok(ApiResponse.success(proof));
    }

    @Operation(summary = "Get proof of delivery of a job",
            description = "Returns the proof of delivery filed for one delivery job.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proof of delivery found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No proof of delivery filed for this job"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> getProofByJobId(
            @Parameter(description = "ID of the delivery job") @PathVariable Long jobId) {
        ProofOfDelivery proof = logisticsService.getProofByJobId(jobId);
        return ResponseEntity.ok(ApiResponse.success(proof));
    }

    @Operation(summary = "List proofs of delivery of a driver",
            description = "Returns the proofs of delivery filed against the jobs of one driver.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proofs of delivery returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<ProofOfDelivery>>> getProofsByDriver(
            @Parameter(description = "ID of the driver") @PathVariable Long driverId) {
        List<ProofOfDelivery> proofs = logisticsService.getProofsByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(proofs));
    }

    @Operation(summary = "Delete proof of delivery", description = "Deletes a proof of delivery.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proof of delivery deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proof of delivery not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProof(
            @Parameter(description = "ID of the proof of delivery") @PathVariable Long id) {
        logisticsService.deleteProof(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Proof of delivery deleted successfully"));
    }
}
