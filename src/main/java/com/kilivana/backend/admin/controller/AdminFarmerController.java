package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.farm.dto.FarmerListItem;
import com.kilivana.backend.farm.dto.FarmerResponse;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.farm.dto.FarmResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/farmers")
@RequiredArgsConstructor
public class AdminFarmerController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;

    @Operation(summary = "Suspend a farmer",
            description = "Suspends the farmer account. The account stays; the farmer cannot sell or interact with the platform.")
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendFarmer(
            @PathVariable("userId") Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(userService.suspendUser(userId, reason)));
    }

    @Operation(summary = "Unsuspend a farmer",
            description = "Reinstates a suspended farmer account.")
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<UserResponse>> unsuspendFarmer(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.activateUser(userId)));
    }

    @Operation(summary = "List farmers")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFarmers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) UserStatus accountStatus,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            Pageable pageable) {

        List<User> farmers = userRepository.findByRole(UserRole.FARMER);
        List<FarmerListItem> items = farmers.stream()
                .filter(u -> matchesSearch(u, search))
                .filter(u -> matchesCounty(u, county))
                .filter(u -> matchesStatus(u, accountStatus))
                .filter(u -> matchesVerification(u, verificationStatus))
                .map(this::toListItem)
                .collect(Collectors.toList());

        int start = Math.max(0, Math.min((int) pageable.getOffset(), items.size()));
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<FarmerListItem> pageItems = items.subList(start, end);
        Page<FarmerListItem> page = new PageImpl<>(pageItems, pageable, items.size());

        Map<String, Object> result = Map.of(
                "items", page.getContent(),
                "page", page.getNumber() + 1,
                "size", page.getSize(),
                "totalItems", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Farmer stats")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        List<User> farmers = userRepository.findByRole(UserRole.FARMER);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", farmers.size());
        stats.put("verified", farmers.stream().filter(f -> f.getVerificationStatus() == VerificationStatus.VERIFIED).count());
        stats.put("pending", farmers.stream().filter(f -> f.getVerificationStatus() == VerificationStatus.PENDING).count());
        stats.put("suspended", farmers.stream().filter(f -> f.getStatus() == UserStatus.SUSPENDED).count());
        stats.put("rejected", farmers.stream().filter(f -> f.getVerificationStatus() == VerificationStatus.REJECTED).count());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get a farmer by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmerResponse>> getFarmer(@PathVariable Long id) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        return ResponseEntity.ok(ApiResponse.success(toFarmerResponse(user)));
    }

    @Operation(summary = "Approve a farmer")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<FarmerResponse>> approveFarmer(@PathVariable Long id) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BadRequestException("Cannot approve a suspended farmer. Reinstate the account first.");
        }
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        return ResponseEntity.ok(ApiResponse.success(toFarmerResponse(userRepository.save(user))));
    }

    @Operation(summary = "Reject a farmer")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<FarmerResponse>> rejectFarmer(
            @PathVariable Long id, @RequestParam String reason) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A rejection reason is required");
        }
        user.setVerificationStatus(VerificationStatus.REJECTED);
        user.setStatus(UserStatus.INACTIVE);
        return ResponseEntity.ok(ApiResponse.success(toFarmerResponse(userRepository.save(user))));
    }

    private boolean matchesSearch(User u, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.toLowerCase(Locale.ROOT);
        return u.getName().toLowerCase().contains(q)
                || u.getEmail().toLowerCase().contains(q)
                || u.getPhone().toLowerCase().contains(q)
                || (u.getReferenceCode() != null && u.getReferenceCode().toLowerCase().contains(q));
    }

    private boolean matchesCounty(User u, String county) {
        if (county == null || county.isBlank()) return true;
        return county.equalsIgnoreCase(u.getRegion());
    }

    private boolean matchesStatus(User u, UserStatus status) {
        if (status == null) return true;
        return u.getStatus() == status;
    }

    private boolean matchesVerification(User u, VerificationStatus verificationStatus) {
        if (verificationStatus == null) return true;
        return u.getVerificationStatus() == verificationStatus;
    }

    private FarmerResponse toFarmerResponse(User user) {
        List<Farm> farms = farmRepository.findByFarmerId(user.getId());
        List<FarmResponse> farmResponses = farms.stream()
                .map(f -> FarmResponse.builder()
                        .id(f.getId())
                        .farmerId(f.getFarmerId())
                        .name(f.getName())
                        .county(f.getCounty())
                        .subCounty(f.getSubCounty())
                        .address(f.getAddress())
                        .latitude(f.getLatitude())
                        .longitude(f.getLongitude())
                        .sizeAcres(f.getSizeAcres())
                        .ownershipType(f.getOwnershipType())
                        .status(f.getStatus())
                        .description(f.getDescription())
                        .createdAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FarmerResponse.builder()
                .id(user.getId())
                .referenceCode(user.getReferenceCode())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .username(user.getUsername())
                .county(user.getRegion())
                .verificationStatus(user.getVerificationStatus())
                .accountStatus(user.getStatus())
                .createdAt(user.getCreatedAt())
                .farms(farmResponses)
                .build();
    }

    private FarmerListItem toListItem(User user) {
        List<Farm> farms = farmRepository.findByFarmerId(user.getId());
        return FarmerListItem.builder()
                .id(user.getId())
                .referenceCode(user.getReferenceCode())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .county(user.getRegion())
                .accountStatus(user.getStatus())
                .verificationStatus(user.getVerificationStatus())
                .statusReason(user.getSuspensionReason())
                .statusChangedAt(user.getStatusChangedAt())
                .statusChangedBy(user.getStatusChangedBy())
                .farmCount(farms.size())
                .createdAt(user.getCreatedAt())
                .rating(null)
                .ratingCount(0)
                .totalRevenue(0.0)
                .build();
    }
}
