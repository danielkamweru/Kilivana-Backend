package com.kilivana.backend.farm.controller;

import com.kilivana.backend.farm.dto.FarmImageResponse;
import com.kilivana.backend.farm.dto.FarmRequest;
import com.kilivana.backend.farm.dto.FarmResponse;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.entity.FarmImage;
import com.kilivana.backend.farm.repository.FarmImageRepository;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.service.ImageStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Farmer-facing farm management endpoints.
 * Allows authenticated farmers to create and manage their own farms.
 */
@Tag(name = "Farmer · Farms", description = "Farmer-facing farm management")
@RestController
@org.springframework.security.access.prepost.PreAuthorize("hasRole('FARMER')")
@RequestMapping("/api/v1/farmers/me/farms")
@RequiredArgsConstructor
public class FarmerFarmController {

    private final FarmRepository farmRepository;
    private final FarmImageRepository farmImageRepository;
    private final ImageStorage imageStorage;

    @Operation(summary = "Create a farm",
            description = "Creates a new farm for the authenticated farmer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Farm created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @AuthenticationPrincipal Long authenticatedUserId,
            @Valid @RequestBody FarmRequest request) {
        Farm farm = Farm.builder()
                .farmerId(authenticatedUserId)
                .name(request.getName())
                .county(request.getCounty())
                .subCounty(request.getSubCounty())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .sizeAcres(request.getSizeAcres())
                .ownershipType(request.getOwnershipType())
                .status(FarmStatus.ACTIVE)
                .description(request.getDescription())
                .build();
        Farm saved = farmRepository.save(farm);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toFullResponse(saved)));
    }

    @Operation(summary = "List my farms",
            description = "Returns all farms belonging to the authenticated farmer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Farms returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmResponse>>> listMyFarms(
            @AuthenticationPrincipal Long authenticatedUserId) {
        List<Farm> farms = farmRepository.findByFarmerId(authenticatedUserId);
        List<FarmResponse> responses = farms.stream()
                .map(this::toFullResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "Get my farm by id",
            description = "Returns a specific farm belonging to the authenticated farmer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Farm returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Farm does not belong to you"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Farm not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> getMyFarm(
            @AuthenticationPrincipal Long authenticatedUserId,
            @Parameter(description = "ID of the farm") @PathVariable Long id) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        if (!farm.getFarmerId().equals(authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only access your own farms");
        }
        return ResponseEntity.ok(ApiResponse.success(toFullResponse(farm)));
    }

    @Operation(summary = "Update my farm",
            description = "Updates a farm belonging to the authenticated farmer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Farm updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Farm does not belong to you"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Farm not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateMyFarm(
            @AuthenticationPrincipal Long authenticatedUserId,
            @Parameter(description = "ID of the farm to update") @PathVariable Long id,
            @Valid @RequestBody FarmRequest request) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        if (!farm.getFarmerId().equals(authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only update your own farms");
        }
        farm.setName(request.getName());
        farm.setCounty(request.getCounty());
        farm.setSubCounty(request.getSubCounty());
        farm.setAddress(request.getAddress());
        farm.setLatitude(request.getLatitude());
        farm.setLongitude(request.getLongitude());
        farm.setSizeAcres(request.getSizeAcres());
        farm.setOwnershipType(request.getOwnershipType());
        farm.setDescription(request.getDescription());
        Farm saved = farmRepository.save(farm);
        return ResponseEntity.ok(ApiResponse.success(toFullResponse(saved)));
    }

    @Operation(summary = "Upload a farm image",
            description = "Uploads an image for one of the authenticated farmer's farms. "
                    + "The first image is made primary automatically.")
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FarmImageResponse>> uploadFarmImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) Boolean isPrimary) throws IOException {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        if (!farm.getFarmerId().equals(authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only add images to your own farms");
        }

        boolean primary = Boolean.TRUE.equals(isPrimary)
                || farmImageRepository.findByFarmIdAndIsPrimaryTrue(id).isEmpty();
        if (primary) {
            farmImageRepository.findByFarmIdAndIsPrimaryTrue(id).ifPresent(existing -> {
                existing.setIsPrimary(false);
                farmImageRepository.save(existing);
            });
        }

        Map<String, Object> upload = imageStorage.uploadImage(image, "kilivana/farms/" + id);
        FarmImage saved = farmImageRepository.save(FarmImage.builder()
                .farmId(id)
                .url(imageStorage.getSecureUrl(upload))
                .sortOrder(sortOrder == null ? 0 : sortOrder)
                .isPrimary(primary)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                FarmImageResponse.builder()
                        .id(saved.getId())
                        .url(saved.getUrl())
                        .isPrimary(saved.getIsPrimary())
                        .build()));
    }

    private FarmResponse toFullResponse(Farm farm) {
        List<FarmImage> images = farmImageRepository.findByFarmIdOrderBySortOrderAsc(farm.getId());
        List<FarmImageResponse> imageResponses = images.stream()
                .map(img -> FarmImageResponse.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .isPrimary(img.getIsPrimary())
                        .build())
                .collect(Collectors.toList());
        return FarmResponse.builder()
                .id(farm.getId())
                .farmerId(farm.getFarmerId())
                .name(farm.getName())
                .county(farm.getCounty())
                .subCounty(farm.getSubCounty())
                .address(farm.getAddress())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .sizeAcres(farm.getSizeAcres())
                .ownershipType(farm.getOwnershipType())
                .status(farm.getStatus())
                .description(farm.getDescription())
                .images(imageResponses)
                .createdAt(farm.getCreatedAt())
                .build();
    }
}
