package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a KYC document submitted by a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO representing a KYC document submitted by a user")
public class KycDocumentResponse {
    @Schema(description = "Unique identifier of the KYC document record", example = "1")
    private Long id;

    @Schema(description = "Identifier of the user who submitted the document", example = "12")
    private Long userId;

    @Schema(description = "Type of the document", example = "NATIONAL_ID")
    private DocumentType documentType;

    @Schema(description = "URL of the uploaded document", example = "https://example.com/documents/id123.pdf")
    private String url;

    @Schema(description = "Current review status of the document", example = "APPROVED")
    private String status;

    @Schema(description = "Reason for rejection, if the document was rejected", example = "Document expired")
    private String rejectionReason;

    @Schema(description = "Identifier of the user who reviewed the document", example = "1")
    private Long reviewedBy;

    @Schema(description = "Timestamp when the document was reviewed", example = "2024-02-10T14:00:00")
    private LocalDateTime reviewedAt;

    @Schema(description = "Timestamp when the document was uploaded", example = "2024-02-01T10:00:00")
    private LocalDateTime uploadedAt;

    @Schema(description = "Timestamp when the document record was created", example = "2024-02-01T10:00:00")
    private LocalDateTime createdAt;
}
