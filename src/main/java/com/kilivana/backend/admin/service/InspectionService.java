package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.InspectionRequest;
import com.kilivana.backend.admin.dto.InspectionResponse;
import com.kilivana.backend.admin.entity.Inspection;
import com.kilivana.backend.admin.entity.InspectionEvidenceImage;
import com.kilivana.backend.admin.repository.InspectionEvidenceImageRepository;
import com.kilivana.backend.admin.repository.InspectionRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.dto.ImageResponse;
import com.kilivana.backend.common.enums.InspectionResult;
import com.kilivana.backend.common.enums.InspectionStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.service.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
    @RequiredArgsConstructor
    public class InspectionService {

    /**
     * Creates, queries and closes inspections, and manages their evidence images.
     *
     * <p>An inspection targets a farmer, supplier or driver and records the inspector's finding
     * (PASS/FAIL) with optional notes and photos. Evidence URLs are also mirrored as a
     * comma-separated string on the inspection row so the list view can show a thumbnail without
     * joining the image table.
     */

    private final InspectionRepository inspectionRepository;
    private final InspectionEvidenceImageRepository evidenceImageRepository;
    private final ImageStorage cloudinaryService;

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
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", id));
        // Delete evidence images from Cloudinary first, then the DB rows, then the
        // inspection itself — so a partial failure leaves the inspection but not orphan blobs.
        evidenceImageRepository.findByInspectionIdOrderByCreatedAt(id).forEach(evidenceImageRepository::delete);
        inspectionRepository.delete(inspection);
    }

    @Transactional
    public List<ImageResponse> uploadEvidenceImage(Long inspectionId, MultipartFile image, Boolean isPrimary) throws IOException {
        inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", inspectionId));

        Map<String, Object> uploadResult = cloudinaryService.uploadImage(image, "inspections/evidence");
        String url = cloudinaryService.getSecureUrl(uploadResult);
        String publicId = cloudinaryService.getPublicId(uploadResult);
        String assetId = cloudinaryService.getAssetId(uploadResult);

        InspectionEvidenceImage entity = InspectionEvidenceImage.builder()
                .inspectionId(inspectionId)
                .url(url)
                .publicId(publicId)
                .assetId(assetId)
                .isPrimary(isPrimary != null && isPrimary)
                .build();

        try {
            entity = evidenceImageRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            cloudinaryService.deleteImage(publicId);
            throw e;
        }

        appendEvidenceUrl(inspectionId, url);

        return listEvidenceImages(inspectionId);
    }

    @Transactional
    public List<ImageResponse> listEvidenceImages(Long inspectionId) {
        return evidenceImageRepository.findByInspectionIdOrderByCreatedAt(inspectionId).stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEvidenceImage(Long inspectionId, Long imageId) throws IOException {
        InspectionEvidenceImage image = evidenceImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("InspectionEvidenceImage", imageId));
        if (!image.getInspectionId().equals(inspectionId)) {
            throw new BadRequestException("Image does not belong to this inspection");
        }
        cloudinaryService.deleteImage(image.getPublicId());
        evidenceImageRepository.delete(image);
        removeEvidenceUrl(inspectionId, image.getUrl());
    }

    private void appendEvidenceUrl(Long inspectionId, String url) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", inspectionId));
        String current = inspection.getEvidenceUrls();
        String updated = (current == null || current.isBlank())
                ? url
                : current + "," + url;
        inspection.setEvidenceUrls(updated);
        inspectionRepository.save(inspection);
    }

    private void removeEvidenceUrl(Long inspectionId, String url) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", inspectionId));
        String current = inspection.getEvidenceUrls();
        if (current != null && !current.isBlank()) {
            List<String> urls = List.of(current.split(","));
            String updated = urls.stream()
                    .map(String::trim)
                    .filter(u -> !u.equals(url))
                    .collect(Collectors.joining(","));
            inspection.setEvidenceUrls(updated.isEmpty() ? null : updated);
            inspectionRepository.save(inspection);
        }
    }

    private ImageResponse toImageResponse(InspectionEvidenceImage image) {
        return ImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .publicId(image.getPublicId())
                .assetId(image.getAssetId())
                .sortOrder(image.getSortOrder())
                .isPrimary(image.getIsPrimary())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
