package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.InspectionRequest;
import com.kilivana.backend.admin.dto.InspectionResponse;
import com.kilivana.backend.admin.entity.Inspection;
import com.kilivana.backend.admin.service.InspectionService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.dto.ImageResponse;
import com.kilivana.backend.common.enums.InspectionStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Quality inspection lifecycle and evidence. Covers creating inspections,
 * filtering them by inspector, status or inspected target, recording
 * results, and uploading, listing and deleting evidence photos. Dual-mapped
 * under the admin path (ADMIN role required) and the plain inspections
 * path (any authenticated user).
 */
@Tag(name = "Administration · Inspections", description = "Quality inspection lifecycle, evidence and result recording")
@RestController
@RequestMapping({"/api/v1/admin/inspections", "/api/v1/inspections"})
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    /**
     * Creates a new inspection.
     *
     * @param request the inspection details
     * @return the created inspection
     */
    @Operation(summary = "Create an inspection",
            description = "Registers a new quality inspection against a target entity.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Inspection created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body or invalid enum value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<InspectionResponse>> createInspection(@RequestBody InspectionRequest request) {
        InspectionResponse inspection = inspectionService.createInspection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(inspection));
    }

    /**
     * Fetches a single inspection by id.
     *
     * @param id the inspection id
     * @return the inspection
     */
    @Operation(summary = "Get an inspection by id",
            description = "Returns a single inspection.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inspection not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionResponse>> getInspectionById(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id) {
        InspectionResponse inspection = inspectionService.getInspectionById(id);
        return ResponseEntity.ok(ApiResponse.success(inspection));
    }

    /**
     * Lists every inspection, paginated.
     *
     * @param pageable pagination and sorting
     * @return a page of inspections
     */
    @Operation(summary = "List inspections",
            description = "Returns every inspection, paginated.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspections listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getAllInspections(Pageable pageable) {
        List<InspectionResponse> inspections = inspectionService.getAllInspections(pageable);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    /**
     * Lists every inspection assigned to one inspector.
     *
     * @param inspectorId the inspector's user id
     * @return the inspector's inspections
     */
    @Operation(summary = "List inspections by inspector",
            description = "Returns every inspection assigned to the given inspector.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspections listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid inspector id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping("/inspector/{inspectorId}")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getInspectionsByInspector(
            @Parameter(description = "Inspector user id", example = "1") @PathVariable Long inspectorId) {
        List<InspectionResponse> inspections = inspectionService.getInspectionsByInspector(inspectorId);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    /**
     * Lists every inspection with a given status.
     *
     * @param status the inspection status
     * @return matching inspections
     */
    @Operation(summary = "List inspections by status",
            description = "Returns every inspection with the given status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspections listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown status value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getInspectionsByStatus(
            @Parameter(description = "Inspection status", example = "PENDING") @PathVariable InspectionStatus status) {
        List<InspectionResponse> inspections = inspectionService.getInspectionsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    /**
     * Lists every inspection recorded against a target entity.
     *
     * @param targetType the kind of target, e.g. FARM or PRODUCT
     * @param targetId   the target's id
     * @return matching inspections
     */
    @Operation(summary = "List inspections by target",
            description = "Returns every inspection recorded against the given target type and id.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspections listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid target id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getInspectionsByTarget(
            @Parameter(description = "Type of the inspected target, e.g. FARM or PRODUCT") @PathVariable String targetType,
            @Parameter(description = "Id of the inspected target", example = "1") @PathVariable Long targetId) {
        List<InspectionResponse> inspections = inspectionService.getInspectionsByTarget(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    /**
     * Sets the status of an inspection.
     *
     * @param id     the inspection id
     * @param status the new status
     * @return the updated inspection
     */
    @Operation(summary = "Update inspection status",
            description = "Sets the status of an inspection.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid status value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inspection not found")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InspectionResponse>> updateInspectionStatus(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id,
            @Parameter(description = "New status", example = "IN_PROGRESS") @RequestParam InspectionStatus status) {
        InspectionResponse inspection = inspectionService.updateInspectionStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(inspection));
    }

    /**
     * Records the outcome of an inspection.
     *
     * @param id      the inspection id
     * @param result  the inspection result
     * @param request optional new notes and evidence URLs
     * @return the updated inspection
     */
    @Operation(summary = "Update inspection result",
            description = "Records the outcome of an inspection and optionally updates its notes and evidence URLs.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection result updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown result value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inspection not found")
    })
    @PutMapping("/{id}/result")
    public ResponseEntity<ApiResponse<InspectionResponse>> updateInspectionResult(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id,
            @Parameter(description = "Inspection result", example = "PASS") @RequestParam String result,
            @RequestBody InspectionRequest request) {
        InspectionResponse inspection = inspectionService.updateInspectionResult(id, result, request);
        return ResponseEntity.ok(ApiResponse.success(inspection));
    }

    /**
     * Records the outcome of an inspection (POST alias of PUT /{id}/result).
     *
     * @param id      the inspection id
     * @param result  the inspection result
     * @param request optional new notes and evidence URLs
     * @return the updated inspection
     */
    @Operation(summary = "Submit inspection result (POST)",
            description = "POST alias for recording the outcome of an inspection.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection result updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown result value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inspection not found")
    })
    @PostMapping("/{id}/result")
    public ResponseEntity<ApiResponse<InspectionResponse>> submitInspectionResult(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id,
            @Parameter(description = "Inspection result", example = "PASS") @RequestParam String result,
            @RequestBody InspectionRequest request) {
        return updateInspectionResult(id, result, request);
    }

    /**
     * Records evidence for an inspection, setting its result to CHANGES_REQUIRED.
     *
     * @param id      the inspection id
     * @param request the evidence details
     * @return the updated inspection
     */
    @Operation(summary = "Submit inspection evidence",
            description = "Records evidence for an inspection, setting its result to CHANGES_REQUIRED.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection evidence recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inspection not found")
    })
    @PostMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<InspectionResponse>> submitEvidence(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id,
            @RequestBody InspectionRequest request) {
        return updateInspectionResult(id, "CHANGES_REQUIRED", request);
    }

    /**
     * Uploads an evidence photo for an inspection.
     *
     * @param id        the inspection id
     * @param image     the evidence photo
     * @param isPrimary whether the photo is the primary evidence
     * @return the inspection's evidence images
     */
    @Operation(summary = "Upload inspection evidence image",
            description = "Uploads an evidence photo to an inspection. The image URL is appended to "
                    + "the inspection's evidenceUrls and returned to the caller.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Evidence image uploaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing image part or data constraint violation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inspection not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "Image exceeds the maximum allowed size"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Image storage operation failed")
    })
    @PostMapping(value = "/{id}/evidence/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadEvidenceImage(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id,
            @RequestPart("image") MultipartFile image,
            @Parameter(description = "Mark the image as the primary evidence photo", example = "true") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = inspectionService.uploadEvidenceImage(id, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    /**
     * Lists an inspection's evidence images.
     *
     * @param id the inspection id
     * @return the inspection's evidence images
     */
    @Operation(summary = "List inspection evidence images",
            description = "Returns every evidence image uploaded for an inspection.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Evidence images listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)")
    })
    @GetMapping("/{id}/evidence/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> listEvidenceImages(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id) {
        List<ImageResponse> images = inspectionService.listEvidenceImages(id);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    /**
     * Deletes one evidence image of an inspection.
     *
     * @param id      the inspection id
     * @param imageId the evidence image id
     */
    @Operation(summary = "Delete an inspection evidence image",
            description = "Removes one evidence image from an inspection and deletes it from storage.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Evidence image deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Image does not belong to this inspection"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Evidence image not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Image storage operation failed")
    })
    @DeleteMapping("/{id}/evidence/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvidenceImage(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id,
            @Parameter(description = "Evidence image id", example = "1") @PathVariable Long imageId) throws IOException {
        inspectionService.deleteEvidenceImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Evidence image deleted"));
    }

    /**
     * Deletes an inspection together with its evidence images.
     *
     * @param id the inspection id
     * @return empty response with a success message
     */
    @Operation(summary = "Delete an inspection",
            description = "Deletes an inspection together with its evidence images.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required (admin path)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inspection not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInspection(
            @Parameter(description = "Inspection id", example = "1") @PathVariable Long id) {
        inspectionService.deleteInspection(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Inspection deleted successfully"));
    }
}
