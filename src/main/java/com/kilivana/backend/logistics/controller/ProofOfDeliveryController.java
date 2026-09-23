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

@RestController
@RequestMapping("/api/v1/logistics/proof-of-delivery")
@RequiredArgsConstructor
public class ProofOfDeliveryController {

    private final LogisticsService logisticsService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProofOfDelivery>> createProofOfDelivery(@RequestBody ProofOfDelivery proof) {
        ProofOfDelivery created = logisticsService.createProofOfDelivery(proof);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> getProofById(@PathVariable Long id) {
        ProofOfDelivery proof = logisticsService.getProofById(id);
        return ResponseEntity.ok(ApiResponse.success(proof));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> getProofByJobId(@PathVariable Long jobId) {
        ProofOfDelivery proof = logisticsService.getProofByJobId(jobId);
        return ResponseEntity.ok(ApiResponse.success(proof));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<ProofOfDelivery>>> getProofsByDriver(@PathVariable Long driverId) {
        List<ProofOfDelivery> proofs = logisticsService.getProofsByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(proofs));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProof(@PathVariable Long id) {
        logisticsService.deleteProof(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Proof of delivery deleted successfully"));
    }
}
