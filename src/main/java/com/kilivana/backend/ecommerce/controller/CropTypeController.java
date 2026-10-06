package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.farm.dto.CropTypeDto;
import com.kilivana.backend.farm.entity.CropType;
import com.kilivana.backend.farm.repository.CropTypeRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Public Catalogue", description = "Public reference data for crop types")
@RestController
@RequestMapping("/api/v1/crop-types")
@RequiredArgsConstructor
public class CropTypeController {

    private final CropTypeRepository cropTypeRepository;

    @Operation(summary = "List active crop types")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CropTypeDto>>> listCropTypes() {
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
}
