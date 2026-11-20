package com.kilivana.backend.logistics.fulfilment.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.fulfilment.dto.FulfilmentStatusUpdateRequest;
import com.kilivana.backend.logistics.fulfilment.dto.SellerFulfilmentResponse;
import com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment;
import com.kilivana.backend.logistics.fulfilment.repository.SellerFulfilmentRepository;
import com.kilivana.backend.logistics.fulfilment.service.SellerFulfilmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Farmer-facing fulfilment management. A farmer sees only their own fulfilment
 * records and can update the preparation status of their portion of an order.
 */
@Tag(name = "Farmer Fulfilment", description = "Farmer-scoped order fulfilment records and status")
@RestController
@RequestMapping("/api/v1/farmer/fulfilments")
@RequiredArgsConstructor
public class FarmerFulfilmentController {

    private final SellerFulfilmentService fulfilmentService;
    private final SellerFulfilmentRepository fulfilmentRepository;

    @Operation(summary = "List my fulfilments", description = "Returns all seller fulfilment records for the authenticated farmer.")
    @GetMapping
    @PreAuthorize("@resourceAuthorization.seller(#sellerId) or @resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<SellerFulfilmentResponse>>> listMyFulfilments(
            @RequestParam(required = false) Long sellerId) {
        Long id = sellerId != null ? sellerId : com.kilivana.backend.security.CurrentUser.id();
        List<SellerFulfilmentResponse> responses = fulfilmentService.getFulfilmentsForSeller(id).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "Get fulfilment by ID", description = "Returns a single seller fulfilment record.")
    @GetMapping("/{id}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<SellerFulfilmentResponse>> getFulfilment(@PathVariable Long id) {
        SellerFulfilment fulfilment = fulfilmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SellerFulfilment", id));
        return ResponseEntity.ok(ApiResponse.success(toResponse(fulfilment)));
    }

    @Operation(summary = "Update fulfilment status", description = "Transitions a seller fulfilment to a new preparation status.")
    @PutMapping("/{id}/status")
    @PreAuthorize("@resourceAuthorization.admin() or @resourceAuthorization.seller(#sellerId)")
    public ResponseEntity<ApiResponse<SellerFulfilmentResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Long sellerId,
            @RequestBody FulfilmentStatusUpdateRequest request) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        SellerFulfilment fulfilment = fulfilmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SellerFulfilment", id));

        // Farmer may only update their own fulfilments
        Long effectiveSellerId = sellerId != null ? sellerId : fulfilment.getSellerId();
        boolean isAdmin = com.kilivana.backend.security.CurrentUser.hasRole("ADMIN");
        if (!isAdmin && !effectiveSellerId.equals(actorId)) {
            throw new ForbiddenException(
                    "Farmer may only update their own fulfilment records");
        }

        com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment updated =
                fulfilmentService.updateStatus(id, request.getStatus(), actorId);

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            updated.setNotes(request.getNotes());
            fulfilmentRepository.save(updated);
        }

        return ResponseEntity.ok(ApiResponse.success(toResponse(updated)));
    }

    private SellerFulfilmentResponse toResponse(SellerFulfilment f) {
        return SellerFulfilmentResponse.builder()
                .id(f.getId())
                .orderId(f.getOrderId())
                .sellerId(f.getSellerId())
                .sellerSubtotal(f.getSellerSubtotal())
                .status(f.getStatus())
                .notes(f.getNotes())
                .readyAt(f.getReadyAt())
                .collectedAt(f.getCollectedAt())
                .createdAt(f.getCreatedAt())
                .items(List.of())
                .build();
    }
}
