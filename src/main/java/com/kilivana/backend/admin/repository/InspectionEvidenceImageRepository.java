package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.InspectionEvidenceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to the {@link InspectionEvidenceImage} table. Evidence images are returned in creation
 * order so the panel renders them as the inspector uploaded them.
 */
@Repository
public interface InspectionEvidenceImageRepository extends JpaRepository<InspectionEvidenceImage, Long> {
    List<InspectionEvidenceImage> findByInspectionIdOrderByCreatedAt(Long inspectionId);
}