package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByUserIdOrderByUploadedAtDesc(Long userId);

    List<KycDocument> findByDocumentStatus(com.kilivana.backend.common.enums.DocumentStatus documentStatus);

    long countByUserIdAndDocumentStatus(Long userId, com.kilivana.backend.common.enums.DocumentStatus status);
}
