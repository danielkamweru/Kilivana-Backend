package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.InspectionEvidenceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionEvidenceImageRepository extends JpaRepository<InspectionEvidenceImage, Long> {
    List<InspectionEvidenceImage> findByInspectionIdOrderByCreatedAt(Long inspectionId);
}
