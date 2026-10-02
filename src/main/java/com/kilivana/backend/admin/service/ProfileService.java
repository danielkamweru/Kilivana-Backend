package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.DriverCapacityFormatter;
import com.kilivana.backend.admin.dto.*;
import com.kilivana.backend.admin.entity.*;
import com.kilivana.backend.admin.repository.*;
import com.kilivana.backend.common.dto.ImageResponse;
import com.kilivana.backend.common.entity.BaseImageEntity;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.service.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final FarmerProfileRepository farmerProfileRepository;
    private final BuyerProfileRepository buyerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final InspectorProfileRepository inspectorProfileRepository;
    private final SupplierProfileRepository supplierProfileRepository;
    private final FarmerProfileImageRepository farmerProfileImageRepository;
    private final SupplierProfileImageRepository supplierProfileImageRepository;
    private final DriverProfileImageRepository driverProfileImageRepository;
    private final InspectorProfileImageRepository inspectorProfileImageRepository;
    private final ImageStorage cloudinaryService;

    @Transactional(readOnly = true)
    public FarmerProfileResponse getFarmerProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.FARMER);
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile", userId));
        List<ImageResponse> images = mapProfileImages(farmerProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
        return FarmerProfileResponse.fromEntity(profile, images);
    }

    @Transactional
    public FarmerProfileResponse createFarmerProfile(Long authenticatedUserId, Long userId, FarmerProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
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
    public FarmerProfileResponse updateFarmerProfile(Long authenticatedUserId, Long userId, FarmerProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
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
    public void deleteFarmerProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile", userId));
        farmerProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public BuyerProfileResponse getBuyerProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.BUYER);
        return BuyerProfileResponse.fromEntity(buyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile", userId)));
    }

    @Transactional
    public BuyerProfileResponse createBuyerProfile(Long authenticatedUserId, Long userId, BuyerProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
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
    public BuyerProfileResponse updateBuyerProfile(Long authenticatedUserId, Long userId, BuyerProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        BuyerProfile profile = buyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile", userId));
        ensureRole(userId, UserRole.BUYER);
        profile.setContactDetails(request.getContactDetails());
        profile.setSavedAddresses(request.getSavedAddresses());
        profile.setUpdatedAt(LocalDateTime.now());
        return BuyerProfileResponse.fromEntity(buyerProfileRepository.save(profile));
    }

    @Transactional
    public void deleteBuyerProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        BuyerProfile profile = buyerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer profile", userId));
        buyerProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.DRIVER);
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", userId));
        List<ImageResponse> images = mapProfileImages(driverProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
        return DriverProfileResponse.fromEntity(profile, images);
    }

    @Transactional
    public DriverProfileResponse createDriverProfile(Long authenticatedUserId, Long userId, DriverProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.DRIVER);
        if (driverProfileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Driver profile already exists for user: " + userId);
        }
        DriverProfile profile = DriverProfile.builder()
                .userId(userId)
                .updatedAt(LocalDateTime.now())
                .build();
        applyDriverRequest(profile, request);
        return DriverProfileResponse.fromEntity(driverProfileRepository.save(profile));
    }

    @Transactional
    public DriverProfileResponse updateDriverProfile(Long authenticatedUserId, Long userId, DriverProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", userId));
        ensureRole(userId, UserRole.DRIVER);
        applyDriverRequest(profile, request);
        profile.setUpdatedAt(LocalDateTime.now());
        return DriverProfileResponse.fromEntity(driverProfileRepository.save(profile));
    }

    /**
     * Copies a driver request onto the profile, resolving the two capacity forms and refusing a
     * suspension with no reason.
     *
     * <p>Kept in one place because create and update had drifted into two copies of the same
     * field list, which is how a field ends up settable on create and not on update.
     */
    private void applyDriverRequest(DriverProfile profile, DriverProfileRequest request) {
        if (request.getAvailabilityStatus() == DriverStatus.SUSPENDED
                && (request.getSuspensionReason() == null || request.getSuspensionReason().isBlank())) {
            throw new BadRequestException("A suspension reason is required when a driver is suspended");
        }
        profile.setAddress(request.getAddress());
        profile.setLicenseNumber(request.getLicenseNumber());
        profile.setVehicleType(request.getVehicleType());
        profile.setVehicleNumber(request.getVehicleNumber());
        profile.setVehicleDetails(request.getVehicleDetails());
        profile.setVehicleMake(request.getVehicleMake());
        profile.setVehicleCapacityKg(driverCapacityKg(request));
        profile.setLicenseExpiryDate(request.getLicenseExpiryDate());
        profile.setIdType(request.getIdType());
        profile.setIdNumber(request.getIdNumber());
        profile.setKycStatus(request.getKycStatus() == null ? KycStatus.PENDING : request.getKycStatus());
        profile.setAvailabilityStatus(request.getAvailabilityStatus());
        profile.setSuspensionReason(request.getSuspensionReason());
    }

    /** The kilogram figure wins when both are sent; the text form is the fallback. */
    private Integer driverCapacityKg(DriverProfileRequest request) {
        if (request.getVehicleCapacityKg() != null) {
            return request.getVehicleCapacityKg();
        }
        try {
            return DriverCapacityFormatter.toKilograms(request.getVehicleCapacity());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    @Transactional
    public void deleteDriverProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", userId));
        driverProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId).forEach(image -> {
            cloudinaryService.deleteImage(image.getPublicId());
        });
        driverProfileImageRepository.deleteByUserId(userId);
        driverProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public InspectorProfileResponse getInspectorProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.INSPECTOR);
        InspectorProfile profile = inspectorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector profile", userId));
        List<ImageResponse> images = mapProfileImages(inspectorProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
        return InspectorProfileResponse.fromEntity(profile, images);
    }

    @Transactional
    public InspectorProfileResponse createInspectorProfile(Long authenticatedUserId, Long userId, InspectorProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
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
    public InspectorProfileResponse updateInspectorProfile(Long authenticatedUserId, Long userId, InspectorProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
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
    public void deleteInspectorProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        InspectorProfile profile = inspectorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector profile", userId));
        inspectorProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId).forEach(image -> {
            cloudinaryService.deleteImage(image.getPublicId());
        });
        inspectorProfileImageRepository.deleteByUserId(userId);
        inspectorProfileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public SupplierProfileResponse getSupplierProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.SUPPLIER);
        SupplierProfile profile = supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile", userId));
        List<ImageResponse> images = mapProfileImages(supplierProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
        return SupplierProfileResponse.fromEntity(profile, images);
    }

    @Transactional
    public SupplierProfileResponse createSupplierProfile(Long authenticatedUserId, Long userId, SupplierProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
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
    public SupplierProfileResponse updateSupplierProfile(Long authenticatedUserId, Long userId, SupplierProfileRequest request) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
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
    public void deleteSupplierProfile(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        SupplierProfile profile = supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile", userId));
        supplierProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId).forEach(image -> {
            cloudinaryService.deleteImage(image.getPublicId());
        });
        supplierProfileImageRepository.deleteByUserId(userId);
        supplierProfileRepository.delete(profile);
    }

    @Transactional
    public List<ImageResponse> uploadFarmerProfileImage(Long authenticatedUserId, Long userId, MultipartFile image, Boolean isPrimary) throws IOException {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.FARMER);
        farmerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile", userId));

        if (Boolean.TRUE.equals(isPrimary)) {
            farmerProfileImageRepository.findByUserIdAndIsPrimaryTrue(userId)
                    .ifPresent(existing -> {
                        existing.setIsPrimary(false);
                        farmerProfileImageRepository.save(existing);
                    });
        }

        String folder = "kilivana/farmers/" + userId;
        Map<String, Object> uploadResult = cloudinaryService.uploadImage(image, folder);

        FarmerProfileImage profileImage = FarmerProfileImage.builder()
                .userId(userId)
                .resourceId(userId)
                .url(cloudinaryService.getSecureUrl(uploadResult))
                .publicId(cloudinaryService.getPublicId(uploadResult))
                .assetId(cloudinaryService.getAssetId(uploadResult))
                .sortOrder(0)
                .isPrimary(Boolean.TRUE.equals(isPrimary))
                .build();

        FarmerProfileImage saved = farmerProfileImageRepository.save(profileImage);
        return mapProfileImages(farmerProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional
    public List<ImageResponse> uploadSupplierProfileImage(Long authenticatedUserId, Long userId, MultipartFile image, Boolean isPrimary) throws IOException {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.SUPPLIER);
        supplierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile", userId));

        if (Boolean.TRUE.equals(isPrimary)) {
            supplierProfileImageRepository.findByUserIdAndIsPrimaryTrue(userId)
                    .ifPresent(existing -> {
                        existing.setIsPrimary(false);
                        supplierProfileImageRepository.save(existing);
                    });
        }

        String folder = "kilivana/suppliers/" + userId;
        Map<String, Object> uploadResult = cloudinaryService.uploadImage(image, folder);

        SupplierProfileImage profileImage = SupplierProfileImage.builder()
                .userId(userId)
                .resourceId(userId)
                .url(cloudinaryService.getSecureUrl(uploadResult))
                .publicId(cloudinaryService.getPublicId(uploadResult))
                .assetId(cloudinaryService.getAssetId(uploadResult))
                .sortOrder(0)
                .isPrimary(Boolean.TRUE.equals(isPrimary))
                .build();

        supplierProfileImageRepository.save(profileImage);
        return mapProfileImages(supplierProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional
    public List<ImageResponse> uploadDriverProfileImage(Long authenticatedUserId, Long userId, MultipartFile image, Boolean isPrimary) throws IOException {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.DRIVER);
        driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", userId));

        if (Boolean.TRUE.equals(isPrimary)) {
            driverProfileImageRepository.findByUserIdAndIsPrimaryTrue(userId)
                    .ifPresent(existing -> {
                        existing.setIsPrimary(false);
                        driverProfileImageRepository.save(existing);
                    });
        }

        String folder = "kilivana/drivers/" + userId;
        Map<String, Object> uploadResult = cloudinaryService.uploadImage(image, folder);

        DriverProfileImage profileImage = DriverProfileImage.builder()
                .userId(userId)
                .resourceId(userId)
                .url(cloudinaryService.getSecureUrl(uploadResult))
                .publicId(cloudinaryService.getPublicId(uploadResult))
                .assetId(cloudinaryService.getAssetId(uploadResult))
                .sortOrder(0)
                .isPrimary(Boolean.TRUE.equals(isPrimary))
                .build();

        driverProfileImageRepository.save(profileImage);
        return mapProfileImages(driverProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getFarmerProfileImages(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.FARMER);
        return mapProfileImages(farmerProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getSupplierProfileImages(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.SUPPLIER);
        return mapProfileImages(supplierProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getDriverProfileImages(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.DRIVER);
        return mapProfileImages(driverProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional
    public void deleteFarmerProfileImage(Long authenticatedUserId, Long userId, Long imageId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.FARMER);
        FarmerProfileImage image = farmerProfileImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile image", imageId));
        if (!image.getUserId().equals(userId)) {
            throw new BadRequestException("Image does not belong to this farmer");
        }
        cloudinaryService.deleteImage(image.getPublicId());
        farmerProfileImageRepository.delete(image);
    }

    @Transactional
    public void deleteSupplierProfileImage(Long authenticatedUserId, Long userId, Long imageId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.SUPPLIER);
        SupplierProfileImage image = supplierProfileImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile image", imageId));
        if (!image.getUserId().equals(userId)) {
            throw new BadRequestException("Image does not belong to this supplier");
        }
        cloudinaryService.deleteImage(image.getPublicId());
        supplierProfileImageRepository.delete(image);
    }

    @Transactional
    public void deleteDriverProfileImage(Long authenticatedUserId, Long userId, Long imageId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.DRIVER);
        DriverProfileImage image = driverProfileImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile image", imageId));
        if (!image.getUserId().equals(userId)) {
            throw new BadRequestException("Image does not belong to this driver");
        }
        cloudinaryService.deleteImage(image.getPublicId());
        driverProfileImageRepository.delete(image);
    }

    @Transactional
    public void setPrimaryFarmerProfileImage(Long authenticatedUserId, Long userId, Long imageId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.FARMER);
        FarmerProfileImage image = farmerProfileImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile image", imageId));
        if (!image.getUserId().equals(userId)) {
            throw new BadRequestException("Image does not belong to this farmer");
        }
        farmerProfileImageRepository.findByUserIdAndIsPrimaryTrue(userId)
                .filter(existing -> !existing.getId().equals(imageId))
                .ifPresent(existing -> {
                    existing.setIsPrimary(false);
                    farmerProfileImageRepository.save(existing);
                });
        image.setIsPrimary(true);
        farmerProfileImageRepository.save(image);
    }

    @Transactional
    public List<ImageResponse> uploadInspectorProfileImage(Long authenticatedUserId, Long userId, MultipartFile image, Boolean isPrimary) throws IOException {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.INSPECTOR);
        inspectorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector profile", userId));

        if (Boolean.TRUE.equals(isPrimary)) {
            inspectorProfileImageRepository.findByUserIdAndIsPrimaryTrue(userId)
                    .ifPresent(existing -> {
                        existing.setIsPrimary(false);
                        inspectorProfileImageRepository.save(existing);
                    });
        }

        String folder = "kilivana/inspectors/" + userId;
        Map<String, Object> uploadResult = cloudinaryService.uploadImage(image, folder);

        InspectorProfileImage profileImage = InspectorProfileImage.builder()
                .userId(userId)
                .resourceId(userId)
                .url(cloudinaryService.getSecureUrl(uploadResult))
                .publicId(cloudinaryService.getPublicId(uploadResult))
                .assetId(cloudinaryService.getAssetId(uploadResult))
                .sortOrder(0)
                .isPrimary(Boolean.TRUE.equals(isPrimary))
                .build();

        inspectorProfileImageRepository.save(profileImage);
        return mapProfileImages(inspectorProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getInspectorProfileImages(Long authenticatedUserId, Long userId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.INSPECTOR);
        return mapProfileImages(inspectorProfileImageRepository.findByUserIdOrderBySortOrderAsc(userId));
    }

    @Transactional
    public void deleteInspectorProfileImage(Long authenticatedUserId, Long userId, Long imageId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.INSPECTOR);
        InspectorProfileImage image = inspectorProfileImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile image", imageId));
        if (!image.getUserId().equals(userId)) {
            throw new BadRequestException("Image does not belong to this inspector");
        }
        cloudinaryService.deleteImage(image.getPublicId());
        inspectorProfileImageRepository.delete(image);
    }

    @Transactional
    public void setPrimaryInspectorProfileImage(Long authenticatedUserId, Long userId, Long imageId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.INSPECTOR);
        InspectorProfileImage image = inspectorProfileImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile image", imageId));
        if (!image.getUserId().equals(userId)) {
            throw new BadRequestException("Image does not belong to this inspector");
        }
        inspectorProfileImageRepository.findByUserIdAndIsPrimaryTrue(userId)
                .filter(existing -> !existing.getId().equals(imageId))
                .ifPresent(existing -> {
                    existing.setIsPrimary(false);
                    inspectorProfileImageRepository.save(existing);
                });
        image.setIsPrimary(true);
        inspectorProfileImageRepository.save(image);
    }

    @Transactional
    public void setPrimarySupplierProfileImage(Long authenticatedUserId, Long userId, Long imageId) {
        ensureOwnershipOrAdmin(authenticatedUserId, userId);
        ensureRole(userId, UserRole.SUPPLIER);
        SupplierProfileImage image = supplierProfileImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile image", imageId));
        if (!image.getUserId().equals(userId)) {
            throw new BadRequestException("Image does not belong to this supplier");
        }
        supplierProfileImageRepository.findByUserIdAndIsPrimaryTrue(userId)
                .filter(existing -> !existing.getId().equals(imageId))
                .ifPresent(existing -> {
                    existing.setIsPrimary(false);
                    supplierProfileImageRepository.save(existing);
                });
        image.setIsPrimary(true);
        supplierProfileImageRepository.save(image);
    }

    private List<ImageResponse> mapProfileImages(List<? extends BaseImageEntity> images) {
        return images.stream()
                .map(image -> ImageResponse.builder()
                        .id(image.getId())
                        .url(image.getUrl())
                        .publicId(image.getPublicId())
                        .assetId(image.getAssetId())
                        .sortOrder(image.getSortOrder())
                        .isPrimary(image.getIsPrimary())
                        .createdAt(image.getCreatedAt())
                        .updatedAt(image.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void ensureRole(Long userId, UserRole expectedRole) {
        UserRole actualRole = userService.getUserById(userId).getRole();
        if (actualRole != expectedRole) {
            throw new BadRequestException("User " + userId + " must have role " + expectedRole);
        }
    }

    private void ensureOwnershipOrAdmin(Long authenticatedUserId, Long profileUserId) {
        if (profileUserId.equals(authenticatedUserId)) {
            return;
        }
        UserRole role = userService.getUserById(authenticatedUserId).getRole();
        if (!role.isStaff()) {
            throw new ForbiddenException("You do not have permission to access this resource");
        }
    }
}