package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.InspectionRequest;
import com.kilivana.backend.admin.dto.InspectionResponse;
import com.kilivana.backend.admin.entity.Inspection;
import com.kilivana.backend.admin.repository.InspectionRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.InspectionResult;
import com.kilivana.backend.common.enums.InspectionStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;

    @Transactional
    public InspectionResponse createInspection(InspectionRequest request) {
        Inspection inspection = Inspection.builder()
                .inspectorId(request.getInspectorId())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .status(request.getStatus())
                .result(request.getResult())
                .notes(request.getNotes())
                .evidenceUrls(request.getEvidenceUrls())
                .build();
        Inspection saved = inspectionRepository.save(inspection);
        return InspectionResponse.fromEntity(saved);
    }

    public InspectionResponse getInspectionById(Long id) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", id));
        return InspectionResponse.fromEntity(inspection);
    }

    public List<InspectionResponse> getAllInspections(Pageable pageable) {
        return inspectionRepository.findAll(pageable).stream()
                .map(InspectionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InspectionResponse> getInspectionsByInspector(Long inspectorId) {
        return inspectionRepository.findByInspectorId(inspectorId).stream()
                .map(InspectionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InspectionResponse> getInspectionsByStatus(InspectionStatus status) {
        return inspectionRepository.findByStatus(status).stream()
                .map(InspectionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InspectionResponse> getInspectionsByTarget(String targetType, Long targetId) {
        return inspectionRepository.findByTargetTypeAndTargetId(targetType, targetId).stream()
                .map(InspectionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public InspectionResponse updateInspectionStatus(Long id, InspectionStatus status) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", id));
        inspection.setStatus(status);
        return InspectionResponse.fromEntity(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionResponse updateInspectionResult(Long id, String result, InspectionRequest request) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", id));
        try {
            inspection.setResult(InspectionResult.valueOf(result));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid inspection result: " + result);
        }
        if (request.getNotes() != null) {
            inspection.setNotes(request.getNotes());
        }
        if (request.getEvidenceUrls() != null) {
            inspection.setEvidenceUrls(request.getEvidenceUrls());
        }
        inspection.setInspectedAt(java.time.LocalDateTime.now());
        return InspectionResponse.fromEntity(inspectionRepository.save(inspection));
    }

    @Transactional
    public void deleteInspection(Long id) {
        if (!inspectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inspection", id);
        }
        inspectionRepository.deleteById(id);
    }
}
