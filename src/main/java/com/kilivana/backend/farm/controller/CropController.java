package com.kilivana.backend.farm.controller;

import com.kilivana.backend.farm.dto.CropResponse;
import com.kilivana.backend.farm.dto.CropSummaryDto;
import com.kilivana.backend.farm.dto.CropTypeDto;
import com.kilivana.backend.farm.entity.Crop;
import com.kilivana.backend.farm.repository.CropRepository;
import com.kilivana.backend.farm.repository.CropTypeRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.CropStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Administration · Crops", description = "Crop catalogue and crop records")
@RestController
@RequestMapping("/api/v1/admin/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropRepository cropRepository;
    private final CropTypeRepository cropTypeRepository;

    @Operation(summary = "List crops")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listCrops(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long cropTypeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) CropStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        Page<Crop> page = cropRepository.searchCrops(search, farmerId, farmId, cropTypeId, status, pageable);
        List<CropResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = Map.of(
                "items", items,
                "page", page.getNumber() + 1,
                "size", page.getSize(),
                "totalItems", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Crop summary for donut chart")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<CropSummaryDto>>> getCropSummary() {
        List<Crop> crops = cropRepository.findAll();
        Map<Long, Long> countsByCropType = crops.stream()
                .collect(Collectors.groupingBy(Crop::getCropTypeId, Collectors.counting()));
        Map<Long, Double> areaByCropType = crops.stream()
                .collect(Collectors.groupingBy(Crop::getCropTypeId, Collectors.summingDouble(Crop::getAreaAcres)));
        Map<Long, Long> yieldByCropType = crops.stream()
                .collect(Collectors.groupingBy(Crop::getCropTypeId,
                        Collectors.summingLong(c -> c.getExpectedYieldKg() != null ? c.getExpectedYieldKg() : 0)));

        List<CropSummaryDto> result = cropTypeRepository.findAll().stream()
                .map(ct -> CropSummaryDto.builder()
                        .cropTypeId(ct.getId())
                        .name(ct.getName())
                        .farmCount(countsByCropType.getOrDefault(ct.getId(), 0L))
                        .totalAreaAcres(areaByCropType.getOrDefault(ct.getId(), 0.0))
                        .expectedYieldKg(yieldByCropType.getOrDefault(ct.getId(), 0L))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Crop catalogue for dropdowns")
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<CropTypeDto>>> getCropTypes() {
        List<CropTypeDto> types = cropTypeRepository.findByActiveTrueOrderByName().stream()
                .map(ct -> CropTypeDto.builder()
                        .id(ct.getId())
                        .name(ct.getName())
                        .category(ct.getCategory())
                        .active(ct.isActive())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @Operation(summary = "Update a crop")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CropResponse>> updateCrop(
            @PathVariable Long id, @RequestBody CropResponse request) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop", id));
        crop.setVariety(request.getVariety());
        crop.setAreaAcres(request.getAreaAcres());
        crop.setStatus(request.getStatus());
        crop.setPlantingDate(request.getPlantingDate());
        crop.setExpectedHarvestDate(request.getExpectedHarvestDate());
        crop.setExpectedYieldKg(request.getExpectedYieldKg());
        Crop saved = cropRepository.save(crop);
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @Operation(summary = "Update crop status")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CropResponse>> updateCropStatus(
            @PathVariable Long id, @RequestParam CropStatus status) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop", id));
        crop.setStatus(status);
        Crop saved = cropRepository.save(crop);
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCrop(@PathVariable Long id) {
        cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop", id));
        cropRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Crop deleted"));
    }

    private CropResponse toResponse(Crop crop) {
        return CropResponse.builder()
                .id(crop.getId())
                .farmId(crop.getFarmId())
                .farmerId(crop.getFarmerId())
                .variety(crop.getVariety())
                .areaAcres(crop.getAreaAcres())
                .status(crop.getStatus())
                .plantingDate(crop.getPlantingDate())
                .expectedHarvestDate(crop.getExpectedHarvestDate())
                .expectedYieldKg(crop.getExpectedYieldKg())
                .createdAt(crop.getCreatedAt())
                .build();
    }
}
