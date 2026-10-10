package com.kilivana.backend.farm.entity;

import com.kilivana.backend.common.enums.DocumentType;
import com.kilivana.backend.common.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a KYC (Know Your Customer) document submitted by a user.
 * <p>
 * KYC documents are part of the farmer onboarding flow. Each document has a
 * {@link DocumentType} (e.g. NATIONAL_ID, KRA_PIN) and a review
 * {@link DocumentStatus} that starts as PENDING. An administrator approves or
 * rejects the document, optionally recording a {@code rejectionReason}. The
 * {@code reviewedBy} and {@code reviewedAt} fields capture who performed the
 * review and when.
 * <p>
 * The {@code uploadedAt} timestamp marks when the document was submitted by
 * the user, while {@code createdAt} is the Hibernate-managed record creation
 * timestamp. In practice these are nearly identical; {@code uploadedAt} is
 * the field surfaced in responses.
 */
@Entity
@Table(name = "kyc_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Type of identity document (e.g. NATIONAL_ID, KRA_PIN). Stored as the
     * enum name so the value is self-describing in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    /**
     * Review status of the document. Defaults to PENDING so newly submitted
     * documents appear in the admin review queue. Transitions to APPROVED or
     * REJECTED are performed via dedicated endpoints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false)
    private DocumentStatus documentStatus = DocumentStatus.PENDING;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    /**
     * Timestamp when the document was uploaded by the user. This is the
     * field surfaced in responses; {@code createdAt} is the Hibernate record
     * creation timestamp and is kept for audit purposes.
     */
    @Column(name = "uploaded_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime uploadedAt;

    /**
     * Reason recorded when the document is rejected. Null when the document
     * is pending or approved.
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Identifier of the administrator who last reviewed the document. Null
     * until the document is approved or rejected.
     */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /**
     * Timestamp when the document was last reviewed. Null until the document
     * is approved or rejected.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}