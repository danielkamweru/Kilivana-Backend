package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.InspectionRequest;
import com.kilivana.backend.admin.dto.InspectionResponse;
import com.kilivana.backend.admin.entity.Inspection;
import com.kilivana.backend.admin.service.InspectionService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.InspectionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/inspections", "/api/v1/inspections"})
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<InspectionResponse>> createInspection(@RequestBody InspectionRequest request) {
        InspectionResponse inspection = inspectionService.createInspection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(inspection));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionResponse>> getInspectionById(@PathVariable Long id) {
        InspectionResponse inspection = inspectionService.getInspectionById(id);
        return ResponseEntity.ok(ApiResponse.success(inspection));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getAllInspections(Pageable pageable) {
        List<InspectionResponse> inspections = inspectionService.getAllInspections(pageable);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    @GetMapping("/inspector/{inspectorId}")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getInspectionsByInspector(@PathVariable Long inspectorId) {
        List<InspectionResponse> inspections = inspectionService.getInspectionsByInspector(inspectorId);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getInspectionsByStatus(@PathVariable InspectionStatus status) {
        List<InspectionResponse> inspections = inspectionService.getInspectionsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getInspectionsByTarget(@PathVariable String targetType, @PathVariable Long targetId) {
        List<InspectionResponse> inspections = inspectionService.getInspectionsByTarget(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InspectionResponse>> updateInspectionStatus(@PathVariable Long id, @RequestParam InspectionStatus status) {
        InspectionResponse inspection = inspectionService.updateInspectionStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(inspection));
    }

    @PutMapping("/{id}/result")
    public ResponseEntity<ApiResponse<InspectionResponse>> updateInspectionResult(@PathVariable Long id, @RequestParam String result, @RequestBody InspectionRequest request) {
        InspectionResponse inspection = inspectionService.updateInspectionResult(id, result, request);
        return ResponseEntity.ok(ApiResponse.success(inspection));
    }

    @PostMapping("/{id}/result")
    public ResponseEntity<ApiResponse<InspectionResponse>> submitInspectionResult(@PathVariable Long id, @RequestParam String result, @RequestBody InspectionRequest request) {
        return updateInspectionResult(id, result, request);
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<InspectionResponse>> submitEvidence(@PathVariable Long id, @RequestBody InspectionRequest request) {
        return updateInspectionResult(id, "CHANGES_REQUIRED", request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInspection(@PathVariable Long id) {
        inspectionService.deleteInspection(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Inspection deleted successfully"));
    }
}
