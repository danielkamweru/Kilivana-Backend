package com.kilivana.backend.farm.controller;

import com.kilivana.backend.farm.dto.FarmResponse;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * Farm roster and details management.
 * Provides endpoints for listing, viewing, updating, and deleting farm records
 * along with status transitions for the farm lifecycle.
 */
@Tag(name = "Administration · Farms", description = "Farm roster and details")
@RestController
@RequestMapping("/api/v1/admin/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmRepository farmRepository;

    @Operation(summary = "List farms",
            description = "Paginated search across farm records with optional filters for farmer, county, status, and free-text search.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of farms returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameter"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFarms(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) FarmStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        // Delegated to the repository's flexible search query; null params are
        // treated as "no filter" so the same query handles broad and narrow searches.
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

    @Operation(summary = "Get a farm by id",
            description = "Returns the full details of a single farm. Returns 404 if the farm does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Farm returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Farm not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarm(@Parameter(description = "ID of the farm") @PathVariable Long id) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        return ResponseEntity.ok(ApiResponse.success(toFullResponse(farm)));
    }

    @Operation(summary = "Update a farm",
            description = "Updates the mutable fields of an existing farm record. Returns 404 if the farm does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Farm updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Farm not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
            @Parameter(description = "ID of the farm to update") @PathVariable Long id, @RequestBody FarmResponse request) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        // Copy all mutable fields from the request DTO onto the managed entity.
        // status and createdAt are intentionally excluded: status is changed via
        // the dedicated status endpoint and createdAt is updatable=false.
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

    @Operation(summary = "Update farm status",
            description = "Changes the status of an existing farm record. Returns 404 if the farm does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Farm status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body or invalid status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Farm not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarmStatus(
            @Parameter(description = "ID of the farm to update") @PathVariable Long id,
            @Parameter(description = "New status for the farm") @RequestParam FarmStatus status) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", id));
        // Status transitions are centralized here so lifecycle rules (e.g. which
        // statuses are terminal) stay in one place instead of being scattered
        // across callers. No validation is performed beyond existence; the enum
        // itself constrains the allowed values.
        farm.setStatus(status);
        Farm saved = farmRepository.save(farm);
        return ResponseEntity.ok(ApiResponse.success(toFullResponse(saved)));
    }

    @Operation(summary = "Delete a farm",
            description = "Permanently removes a farm record. Returns 404 if the farm does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Farm deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Farm not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFarm(@Parameter(description = "ID of the farm to delete") @PathVariable Long id) {
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
