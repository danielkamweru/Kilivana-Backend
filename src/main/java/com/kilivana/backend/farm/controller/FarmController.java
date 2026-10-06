package com.kilivana.backend.farm.controller;

import com.kilivana.backend.farm.dto.FarmResponse;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Administration · Farms", description = "Farm roster and details")
@RestController
@RequestMapping("/api/v1/admin/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmRepository farmRepository;

    @Operation(summary = "List farms")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFarms(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) FarmStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        Page<Farm> page = farmRepository.searchFarms(search, county, status, farmerId, pageable);
        Map<String, Object> result = Map.of(
                "items", page.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                "page", page.getNumber() + 1,
                "size", page.getSize(),
                "totalItems", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Get a farm by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarm(@PathVariable Long id) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        return ResponseEntity.ok(ApiResponse.success(toFullResponse(farm)));
    }

    @Operation(summary = "Update a farm")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
            @PathVariable Long id, @RequestBody FarmResponse request) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
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

    @Operation(summary = "Update farm status")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarmStatus(
            @PathVariable Long id, @RequestParam FarmStatus status) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        farm.setStatus(status);
        Farm saved = farmRepository.save(farm);
        return ResponseEntity.ok(ApiResponse.success(toFullResponse(saved)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFarm(@PathVariable Long id) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        farmRepository.delete(farm);
        return ResponseEntity.ok(ApiResponse.successMessage("Farm deleted"));
    }

    private FarmResponse toResponse(Farm farm) {
        return FarmResponse.builder()
                .id(farm.getId())
                .farmerId(farm.getFarmerId())
                .name(farm.getName())
                .county(farm.getCounty())
                .subCounty(farm.getSubCounty())
                .address(farm.getAddress())
                .sizeAcres(farm.getSizeAcres())
                .ownershipType(farm.getOwnershipType())
                .status(farm.getStatus())
                .createdAt(farm.getCreatedAt())
                .build();
    }

    private FarmResponse toFullResponse(Farm farm) {
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
                .createdAt(farm.getCreatedAt())
                .build();
    }
}
