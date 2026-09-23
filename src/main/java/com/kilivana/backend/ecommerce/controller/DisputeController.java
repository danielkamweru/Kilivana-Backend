package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.DisputeRequest;
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

@RestController
@RequestMapping("/api/v1/ecommerce/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(@RequestBody DisputeRequest request) {
        DisputeResponse dispute = disputeService.createDispute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dispute));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDisputeById(@PathVariable Long id) {
        DisputeResponse dispute = disputeService.getDisputeById(id);
        return ResponseEntity.ok(ApiResponse.success(dispute));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputesByOrder(@PathVariable Long orderId) {
        List<DisputeResponse> disputes = disputeService.getDisputesByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputesByUser(@PathVariable Long userId) {
        List<DisputeResponse> disputes = disputeService.getDisputesByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DisputeResponse>> updateDisputeStatus(@PathVariable Long id, @RequestParam DisputeStatus status) {
        DisputeResponse dispute = disputeService.updateDisputeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(dispute));
    }

    @PutMapping("/{id}/resolution")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolveDispute(@PathVariable Long id, @RequestBody String resolution) {
        DisputeResponse dispute = disputeService.resolveDispute(id, resolution);
        return ResponseEntity.ok(ApiResponse.success(dispute));
    }
}
