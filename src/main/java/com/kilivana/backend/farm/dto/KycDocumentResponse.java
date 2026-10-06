package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentResponse {
    private Long id;
    private Long userId;
    private DocumentType documentType;
    private String url;
    private String status;
    private String rejectionReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
}
