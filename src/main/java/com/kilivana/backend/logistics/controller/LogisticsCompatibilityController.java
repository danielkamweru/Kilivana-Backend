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

@RestController
@RequestMapping("/api/v1/logistics/jobs")
@RequiredArgsConstructor
public class LogisticsCompatibilityController {

    private final LogisticsService logisticsService;

    @PostMapping("/{jobId}/location")
    public ResponseEntity<ApiResponse<TrackingEvent>> submitLocation(
            @PathVariable Long jobId, @RequestBody TrackingEvent event) {
        TrackingEvent created = logisticsService.addTrackingEvent(
                jobId, event.getStatus(), event.getLatitude(), event.getLongitude(), event.getNote(), event.getDriverId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @GetMapping("/{jobId}/tracking")
    public ResponseEntity<ApiResponse<List<TrackingEvent>>> getTracking(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(logisticsService.getTrackingHistory(jobId)));
    }

    @PostMapping("/{jobId}/proof-of-delivery")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> submitProof(
            @PathVariable Long jobId, @RequestBody ProofOfDelivery proof) {
        proof.setLogisticsJobId(jobId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(logisticsService.createProofOfDelivery(proof)));
    }
}