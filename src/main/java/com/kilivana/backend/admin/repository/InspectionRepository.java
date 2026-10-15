package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.Inspection;
import com.kilivana.backend.common.enums.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to the {@link Inspection} table. Inspections target any role through a generic
 * ({@code targetType}, {@code targetId}) pair, so the queries here are by inspector, by status
 * or by that target pair — never by a role-specific foreign key.
 */
@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    List<Inspection> findByInspectorId(Long inspectorId);

    List<Inspection> findByStatus(InspectionStatus status);

    List<Inspection> findByTargetTypeAndTargetId(String targetType, Long targetId);
}