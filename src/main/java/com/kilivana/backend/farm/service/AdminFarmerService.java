package com.kilivana.backend.farm.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.farm.dto.FarmerListItem;
import com.kilivana.backend.farm.dto.FarmerResponse;
import com.kilivana.backend.farm.dto.FarmResponse;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
    @RequiredArgsConstructor
    public class AdminFarmerService {

    /**
     * Administers farmer accounts: listing with filters, approval/rejection,
     * suspension/reinstatement, and farm details.
     *
     * <p>Farmers are distinct from {@link AdminSupplierService} and {@link ProfileService} because
     * they are the only role that owns farms and their verification gate is a manual review by an
     * inspector, not an automatic document check.
     */

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;

    @Transactional(readOnly = true)
    public Page<FarmerListItem> listFarmers(String search, String county, UserStatus accountStatus,
                                            VerificationStatus verificationStatus, Pageable pageable) {
        List<User> farmers = userRepository.findByRole(UserRole.FARMER);
        List<FarmerListItem> items = farmers.stream()
                .filter(u -> matchesSearch(u, search))
                .filter(u -> matchesCounty(u, county))
                .filter(u -> matchesStatus(u, accountStatus))
                .filter(u -> matchesVerification(u, verificationStatus))
                .map(this::toListItem)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<FarmerListItem> pageItems = items.subList(start, end);

        return new PageImpl<>(pageItems, pageable, items.size());
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

    @Transactional(readOnly = true)
    public Map<String, Object> getFarmerStats() {
        List<User> farmers = userRepository.findByRole(UserRole.FARMER);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", farmers.size());
        stats.put("verified", farmers.stream().filter(f -> f.getVerificationStatus() == VerificationStatus.VERIFIED).count());
        stats.put("pending", farmers.stream().filter(f -> f.getVerificationStatus() == VerificationStatus.PENDING).count());
        stats.put("suspended", farmers.stream().filter(f -> f.getStatus() == UserStatus.SUSPENDED).count());
        stats.put("rejected", farmers.stream().filter(f -> f.getVerificationStatus() == VerificationStatus.REJECTED).count());
        return stats;
    }

    @Transactional(readOnly = true)
    public FarmerResponse getFarmerDetails(Long id) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        return toFarmerResponse(user);
    }

    @Transactional
    public FarmerResponse approveFarmer(Long id) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BadRequestException("Cannot approve a suspended farmer. Reinstate the account first.");
        }
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        User saved = userRepository.save(user);
        return toFarmerResponse(saved);
    }

    @Transactional
    public FarmerResponse rejectFarmer(Long id, String reason) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A rejection reason is required");
        }
        user.setVerificationStatus(VerificationStatus.REJECTED);
        user.setStatus(UserStatus.INACTIVE);
        User saved = userRepository.save(user);
        return toFarmerResponse(saved);
    }

    @Transactional
    public FarmerResponse suspendFarmer(Long id, String reason) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A suspension reason is required");
        }
        user.setStatus(UserStatus.SUSPENDED);
        User saved = userRepository.save(user);
        return toFarmerResponse(saved);
    }

    @Transactional
    public FarmerResponse reinstateFarmer(Long id) {
        User user = userRepository.findByIdAndRole(id, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", id));
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        return toFarmerResponse(saved);
    }

    private FarmerResponse toFarmerResponse(User user) {
        List<Farm> farms = farmRepository.findByFarmerId(user.getId());

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
                .farms(farms.stream()
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
                        .collect(Collectors.toList()))
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
