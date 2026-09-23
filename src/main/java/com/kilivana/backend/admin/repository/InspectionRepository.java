package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.Inspection;
import com.kilivana.backend.common.enums.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    
    List<Inspection> findByInspectorId(Long inspectorId);
    
    List<Inspection> findByStatus(InspectionStatus status);
    
    List<Inspection> findByTargetTypeAndTargetId(String targetType, Long targetId);
}
