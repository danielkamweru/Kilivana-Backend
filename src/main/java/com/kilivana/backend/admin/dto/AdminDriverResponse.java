package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row of the administrator's driver list: the user account, the driver profile and the
 * delivery history, in a single read.
 *
 * <p>The admin panel showed all of these together. Fetching them separately meant the list had
 * to be stitched together client-side from a user list, a profile per user and a job list,
 * which meant a driver without a profile either vanished or threw.
 */
@Data
@Builder
public class AdminDriverResponse {

    private Long userId;
    private Long profileId;
    /** Driver code such as DA-005, from the user row. */
    private String code;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String region;
    private String address;

    private DriverStatus status;
    private String suspensionReason;

    // KYC
    private String idType;
    private String idNumber;
    private String licenseNumber;
    private LocalDate licenseExpiryDate;
    private KycStatus kycStatus;

    // Vehicle
    private VehicleType vehicleType;
    private String vehicleCapacity;
    private String plateNumber;
    private String vehicleMake;

    // Delivery history, derived from logistics_jobs
    /** Order id of the delivery in progress, or null when the driver has none. */
    private Long activeOrderId;
    /** Jobs this driver has completed. */
    private long totalDeliveries;
    /**
     * Always null. Nothing in the system records a rating yet, so a value here would be a
     * number the backend cannot support rather than a measurement.
     */
    private Double rating;

    private LocalDateTime createdAt;
}