package com.kilivana.backend.farm.controller;

import com.kilivana.backend.farm.entity.KycDocument;
import com.kilivana.backend.farm.repository.KycDocumentRepository;
import com.kilivana.backend.farm.dto.KycDocumentResponse;
import com.kilivana.backend.common.enums.DocumentStatus;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Administration · KYC", description = "KYC document verification")
@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycDocumentRepository kycDocumentRepository;

    @Operation(summary = "List all KYC documents")
    @GetMapping
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> listDocuments() {
        List<KycDocumentResponse> docs = kycDocumentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @Operation(summary = "List pending KYC documents")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> listPendingDocuments() {
        List<KycDocumentResponse> docs = kycDocumentRepository.findByDocumentStatus(DocumentStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @Operation(summary = "Approve a KYC document")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> approveDocument(@PathVariable Long id) {
        KycDocument doc = kycDocumentRepository.findById(id)
                .orElseThrow(() -> new com.kilivana.backend.common.exception.ResourceNotFoundException("KYC document", id));
        doc.setDocumentStatus(DocumentStatus.APPROVED);
        KycDocument saved = kycDocumentRepository.save(doc);
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @Operation(summary = "Reject a KYC document")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> rejectDocument(
            @PathVariable Long id, @RequestParam String reason) {
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
