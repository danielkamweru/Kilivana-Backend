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

/**
 * Public crop type catalogue.
 * Exposes read-only reference data for crop types, used to populate
 * dropdowns and filters across the farm and e-commerce modules.
 */
@Tag(name = "Public Catalogue", description = "Public reference data for crop types")
@RestController
@RequestMapping("/api/v1/crop-types")
@RequiredArgsConstructor
public class CropTypeController {

    private final CropTypeRepository cropTypeRepository;

    @Operation(summary = "List active crop types",
            description = "Returns all active crop types ordered by name, suitable for populating dropdown controls.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Crop types returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
