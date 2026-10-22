
package com.kilivana.backend.farm.controller;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.farm.dto.FarmImageResponse;
import com.kilivana.backend.farm.dto.FarmOwnerResponse;
import com.kilivana.backend.farm.dto.FarmResponse;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.entity.FarmImage;
import com.kilivana.backend.farm.repository.FarmImageRepository;
import com.kilivana.backend.farm.repository.FarmRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(
        name = "Administration · Farms",
        description = "Farm roster and details"
)
@RestController
@RequestMapping("/api/v1/admin/farms")
@RequiredArgsConstructor
public class FarmController {

    private static final Logger logger =
            LoggerFactory.getLogger(FarmController.class);

    private final FarmRepository farmRepository;
    private final FarmImageRepository farmImageRepository;
    private final UserRepository userRepository;

    // =========================================================
    // LIST FARMS
    // GET /api/v1/admin/farms
    // =========================================================

    @Operation(
            summary = "List farms",
            description = "Returns paginated farm records, including farmer details."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFarms(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) FarmStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        try {
            Page<Farm> page = farmRepository.searchFarms(
                    search,
                    county,
                    status,
                    farmerId,
                    pageable
            );

            List<FarmResponse> farmResponses = page.getContent()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> result = Map.of(
                    "items", farmResponses,
                    "page", page.getNumber() + 1,
                    "size", page.getSize(),
                    "totalItems", page.getTotalElements(),
                    "totalPages", page.getTotalPages()
            );

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("Error listing farms", e);
            throw e;
        }
    }

    // =========================================================
    // GET ONE FARM
    // GET /api/v1/admin/farms/{id}
    // =========================================================

    @Operation(
            summary = "Get a farm by ID",
            description = "Returns farm details, images, and farmer information."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarm(
            @Parameter(description = "ID of the farm")
            @PathVariable Long id) {

        Farm farm = farmRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Farm", id)
                );

        return ResponseEntity.ok(
                ApiResponse.success(toFullResponse(farm))
        );
    }

    // =========================================================
    // UPDATE FARM
    // PUT /api/v1/admin/farms/{id}
    // =========================================================

    @Operation(
            summary = "Update a farm",
            description = "Updates the mutable fields of an existing farm."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
            @Parameter(description = "ID of the farm to update")
            @PathVariable Long id,
            @RequestBody FarmResponse request) {

        Farm farm = farmRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Farm", id)
                );

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

        return ResponseEntity.ok(
                ApiResponse.success(toFullResponse(saved))
        );
    }

    // =========================================================
    // UPDATE FARM STATUS
    // PUT /api/v1/admin/farms/{id}/status?status=ACTIVE
    // =========================================================

    @Operation(
            summary = "Update farm status",
            description = "Changes the status of an existing farm."
    )
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarmStatus(
            @Parameter(description = "ID of the farm")
            @PathVariable Long id,
            @Parameter(description = "New farm status")
            @RequestParam FarmStatus status) {

        Farm farm = farmRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Farm", id)
                );

        farm.setStatus(status);

        Farm saved = farmRepository.save(farm);

        return ResponseEntity.ok(
                ApiResponse.success(toFullResponse(saved))
        );
    }

    // =========================================================
    // DELETE FARM
    // DELETE /api/v1/admin/farms/{id}
    // =========================================================

    @Operation(
            summary = "Delete a farm",
            description = "Permanently removes an existing farm record."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFarm(
            @Parameter(description = "ID of the farm to delete")
            @PathVariable Long id) {

        Farm farm = farmRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Farm", id)
                );

        farmRepository.delete(farm);

        return ResponseEntity.ok(
                ApiResponse.successMessage("Farm deleted")
        );
    }

    // =========================================================
    // MAP FARM TO RESPONSE FOR THE PAGINATED LIST
    // =========================================================

    private FarmResponse toResponse(Farm farm) {

        return FarmResponse.builder()
                .id(farm.getId())
                .farmerId(farm.getFarmerId())
                .farmer(toFarmerResponse(farm.getFarmerId()))
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
                .images(
                        farmImageRepository
                                .findByFarmIdOrderBySortOrderAsc(farm.getId())
                                .stream()
                                .map(this::toImageResponse)
                                .collect(Collectors.toList())
                )
                .createdAt(farm.getCreatedAt())
                .build();
    }

    // =========================================================
    // MAP FARM TO RESPONSE FOR A SINGLE FARM
    // =========================================================

    private FarmResponse toFullResponse(Farm farm) {

        List<FarmImageResponse> imageResponses =
                farmImageRepository
                        .findByFarmIdOrderBySortOrderAsc(farm.getId())
                        .stream()
                        .map(this::toImageResponse)
                        .collect(Collectors.toList());

        return FarmResponse.builder()
                .id(farm.getId())
                .farmerId(farm.getFarmerId())
                .farmer(toFarmerResponse(farm.getFarmerId()))
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

    // =========================================================
    // MAP FARM IMAGE TO RESPONSE
    // =========================================================

    private FarmImageResponse toImageResponse(FarmImage image) {

        return FarmImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    // =========================================================
    // GET FARM OWNER DETAILS
    // =========================================================

    private FarmOwnerResponse toFarmerResponse(Long farmerId) {

        if (farmerId == null) {
            return null;
        }

        return userRepository.findById(farmerId)
                .map(user -> FarmOwnerResponse.builder()
                        .id(user.getId())
                        .referenceCode(user.getReferenceCode())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .county(user.getRegion())
                        .build()
                )
                .orElse(null);
    }
}
