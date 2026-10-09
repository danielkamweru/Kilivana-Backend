package com.kilivana.backend.farm.controller;

import com.kilivana.backend.farm.entity.KycDocument;
import com.kilivana.backend.farm.repository.KycDocumentRepository;
import com.kilivana.backend.farm.dto.KycDocumentResponse;
import com.kilivana.backend.common.enums.DocumentStatus;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * KYC document verification.
 * Provides endpoints for listing, approving, and rejecting KYC documents
 * submitted by users during onboarding.
 */
@Tag(name = "Administration · KYC", description = "KYC document verification")
@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycDocumentRepository kycDocumentRepository;

    @Operation(summary = "List all KYC documents",
            description = "Returns every KYC document regardless of status, ordered by creation date.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC documents returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> listDocuments() {
        List<KycDocumentResponse> docs = kycDocumentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @Operation(summary = "List pending KYC documents",
            description = "Returns only KYC documents whose status is PENDING, ready for review.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending KYC documents returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> listPendingDocuments() {
        List<KycDocumentResponse> docs = kycDocumentRepository.findByDocumentStatus(DocumentStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @Operation(summary = "Approve a KYC document",
            description = "Sets the status of a KYC document to APPROVED. Returns 404 if the document does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC document approved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC document not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> approveDocument(
            @Parameter(description = "ID of the KYC document to approve") @PathVariable Long id) {
        KycDocument doc = kycDocumentRepository.findById(id)
                .orElseThrow(() -> new com.kilivana.backend.common.exception.ResourceNotFoundException("KYC document", id));
        doc.setDocumentStatus(DocumentStatus.APPROVED);
        KycDocument saved = kycDocumentRepository.save(doc);
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @Operation(summary = "Reject a KYC document",
            description = "Sets the status of a KYC document to REJECTED and records the supplied rejection reason. Returns 404 if the document does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC document rejected"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC document not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> rejectDocument(
            @Parameter(description = "ID of the KYC document to reject") @PathVariable Long id,
            @Parameter(description = "Reason for rejecting the KYC document") @RequestParam String reason) {
        KycDocument doc = kycDocumentRepository.findById(id)
                .orElseThrow(() -> new com.kilivana.backend.common.exception.ResourceNotFoundException("KYC document", id));
        doc.setDocumentStatus(DocumentStatus.REJECTED);
        doc.setRejectionReason(reason);
        KycDocument saved = kycDocumentRepository.save(doc);
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    private KycDocumentResponse toResponse(KycDocument doc) {
        return KycDocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .documentType(doc.getDocumentType())
                .url(doc.getUrl())
                .status(doc.getDocumentStatus().name())
                .rejectionReason(doc.getRejectionReason())
                .reviewedBy(doc.getReviewedBy())
                .reviewedAt(doc.getReviewedAt())
                .uploadedAt(doc.getUploadedAt())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
