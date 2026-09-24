package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.*;
import com.kilivana.backend.admin.entity.*;
import com.kilivana.backend.admin.repository.*;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final FarmerProfileRepository farmerProfileRepository;
    private final BuyerProfileRepository buyerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final InspectorProfileRepository inspectorProfileRepository;
    private final SupplierProfileRepository supplierProfileRepository;

    @Transactional(readOnly = true)
    public FarmerProfileResponse getFarmerProfile(Long userId) {
        ensureRole(userId, UserRole.FARMER);
        return FarmerProfileResponse.fromEntity(farmerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile", userId)));
    }

    @Transactional
    public FarmerProfileResponse createFarmerProfile(Long userId, FarmerProfileRequest request) {
        ensureRole(userId, UserRole.FARMER);
        if (farmerProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Farmer profile already exists for user: " + userId);
        }
        FarmerProfile profile = FarmerProfile.builder()
                .userId(userId)
                .farmName(request.getFarmName())
                .location(request.getLocation())
                .farmDetails(request.getFarmDetails())
                .verificationInfo(request.getVerificationInfo())
                .updatedAt(LocalDateTime.now())
                .build();
        return FarmerProfileResponse.fromEntity(farmerProfileRepository.save(profile));
    }

    @Transactional
    public FarmerProfileResponse updateFarmerProfile(Long userId, FarmerProfileRequest request) {
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile", userId));
        ensureRole(userId, UserRole.FARMER);
        profile.setFarmName(request.getFarmName());
        profile.setLocation(request.getLocation());
        profile.setFarmDetails(request.getFarmDetails());
        profile.setVerificationInfo(request.getVerificationInfo());
        profile.setUpdatedAt(LocalDateTime.now());
        return FarmerProfileResponse.fromEntity(farmerProfileRepository.save(profile));
    }

    @Transactional
    public void deleteFarmerProfile(Long userId) {
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile", userId));
        farmerProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public BuyerProfileResponse getBuyerProfile(Long userId) {
        ensureRole(userId, UserRole.BUYER);
        return BuyerProfileResponse.fromEntity(buyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile", userId)));
    }

    @Transactional
    public BuyerProfileResponse createBuyerProfile(Long userId, BuyerProfileRequest request) {
        ensureRole(userId, UserRole.BUYER);
        if (buyerProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Buyer profile already exists for user: " + userId);
        }
        BuyerProfile profile = BuyerProfile.builder()
                .userId(userId)
                .contactDetails(request.getContactDetails())
                .savedAddresses(request.getSavedAddresses())
                .updatedAt(LocalDateTime.now())
                .build();
        return BuyerProfileResponse.fromEntity(buyerProfileRepository.save(profile));
    }

    @Transactional
    public BuyerProfileResponse updateBuyerProfile(Long userId, BuyerProfileRequest request) {
        BuyerProfile profile = buyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile", userId));
        ensureRole(userId, UserRole.BUYER);
        profile.setContactDetails(request.getContactDetails());
        profile.setSavedAddresses(request.getSavedAddresses());
        profile.setUpdatedAt(LocalDateTime.now());
        return BuyerProfileResponse.fromEntity(buyerProfileRepository.save(profile));
    }

    @Transactional
    public void deleteBuyerProfile(Long userId) {
        BuyerProfile profile = buyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile", userId));
        buyerProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverProfile(Long userId) {
        ensureRole(userId, UserRole.DRIVER);
        return DriverProfileResponse.fromEntity(driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", userId)));
    }

    @Transactional
    public DriverProfileResponse createDriverProfile(Long userId, DriverProfileRequest request) {
        ensureRole(userId, UserRole.DRIVER);
        if (driverProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Driver profile already exists for user: " + userId);
        }
        DriverProfile profile = DriverProfile.builder()
                .userId(userId)
                .licenseNumber(request.getLicenseNumber())
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .vehicleDetails(request.getVehicleDetails())
                .availabilityStatus(request.getAvailabilityStatus())
                .updatedAt(LocalDateTime.now())
                .build();
        return DriverProfileResponse.fromEntity(driverProfileRepository.save(profile));
    }

    @Transactional
    public DriverProfileResponse updateDriverProfile(Long userId, DriverProfileRequest request) {
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", userId));
        ensureRole(userId, UserRole.DRIVER);
        profile.setLicenseNumber(request.getLicenseNumber());
        profile.setVehicleType(request.getVehicleType());
        profile.setVehicleNumber(request.getVehicleNumber());
        profile.setVehicleDetails(request.getVehicleDetails());
        profile.setAvailabilityStatus(request.getAvailabilityStatus());
        profile.setUpdatedAt(LocalDateTime.now());
        return DriverProfileResponse.fromEntity(driverProfileRepository.save(profile));
    }

    @Transactional
    public void deleteDriverProfile(Long userId) {
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", userId));
        driverProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public InspectorProfileResponse getInspectorProfile(Long userId) {
        ensureRole(userId, UserRole.INSPECTOR);
        return InspectorProfileResponse.fromEntity(inspectorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector profile", userId)));
    }

    @Transactional
    public InspectorProfileResponse createInspectorProfile(Long userId, InspectorProfileRequest request) {
        ensureRole(userId, UserRole.INSPECTOR);
        if (inspectorProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Inspector profile already exists for user: " + userId);
        }
        InspectorProfile profile = InspectorProfile.builder()
                .userId(userId)
                .inspectorDetails(request.getInspectorDetails())
                .assignedArea(request.getAssignedArea())
                .status(request.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
        return InspectorProfileResponse.fromEntity(inspectorProfileRepository.save(profile));
    }

    @Transactional
    public InspectorProfileResponse updateInspectorProfile(Long userId, InspectorProfileRequest request) {
        InspectorProfile profile = inspectorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector profile", userId));
        ensureRole(userId, UserRole.INSPECTOR);
        profile.setInspectorDetails(request.getInspectorDetails());
        profile.setAssignedArea(request.getAssignedArea());
        profile.setStatus(request.getStatus());
        profile.setUpdatedAt(LocalDateTime.now());
        return InspectorProfileResponse.fromEntity(inspectorProfileRepository.save(profile));
    }

    @Transactional
    public void deleteInspectorProfile(Long userId) {
        InspectorProfile profile = inspectorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector profile", userId));
        inspectorProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public SupplierProfileResponse getSupplierProfile(Long userId) {
        ensureRole(userId, UserRole.SUPPLIER);
        return SupplierProfileResponse.fromEntity(supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile", userId)));
    }

    @Transactional
    public SupplierProfileResponse createSupplierProfile(Long userId, SupplierProfileRequest request) {
        ensureRole(userId, UserRole.SUPPLIER);
        if (supplierProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Supplier profile already exists for user: " + userId);
        }
        SupplierProfile profile = SupplierProfile.builder()
                .userId(userId)
                .businessName(request.getBusinessName())
                .businessDetails(request.getBusinessDetails())
                .location(request.getLocation())
                .verificationInfo(request.getVerificationInfo())
                .updatedAt(LocalDateTime.now())
                .build();
        return SupplierProfileResponse.fromEntity(supplierProfileRepository.save(profile));
    }

    @Transactional
    public SupplierProfileResponse updateSupplierProfile(Long userId, SupplierProfileRequest request) {
        SupplierProfile profile = supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile", userId));
        ensureRole(userId, UserRole.SUPPLIER);
        profile.setBusinessName(request.getBusinessName());
        profile.setBusinessDetails(request.getBusinessDetails());
        profile.setLocation(request.getLocation());
        profile.setVerificationInfo(request.getVerificationInfo());
        profile.setUpdatedAt(LocalDateTime.now());
        return SupplierProfileResponse.fromEntity(supplierProfileRepository.save(profile));
    }

    @Transactional
    public void deleteSupplierProfile(Long userId) {
        SupplierProfile profile = supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile", userId));
        supplierProfileRepository.delete(profile);
    }

    private void ensureRole(Long userId, UserRole expectedRole) {
        UserRole actualRole = userService.getUserById(userId).getRole();
        if (actualRole != expectedRole) {
            throw new BadRequestException("User " + userId + " must have role " + expectedRole);
        }
    }
}