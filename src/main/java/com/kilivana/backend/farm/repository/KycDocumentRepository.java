package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link KycDocument} entities.
 * <p>
 * Supports the KYC review workflow: listing all documents, listing documents
 * for a specific user (ordered by upload date, newest first), listing
 * documents by review status, and counting documents for a user by status
 * (used to detect duplicate submissions).
 */
@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByUserIdOrderByUploadedAtDesc(Long userId);

    List<KycDocument> findByDocumentStatus(com.kilivana.backend.common.enums.DocumentStatus documentStatus);

    long countByUserIdAndDocumentStatus(Long userId, com.kilivana.backend.common.enums.DocumentStatus status);
}