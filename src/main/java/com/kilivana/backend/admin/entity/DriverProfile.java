package com.kilivana.backend.admin.entity;

import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The driver role's profile: licence, vehicle and KYC state. Drivers belong to the logistics
 * workforce, so this row holds the facts a dispatcher needs — capacity, plate and availability.
 */
@Entity
@Table(name = "driver_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The driver's user id; unique because a user may have at most one driver profile. */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** Where the driver lives and collects the vehicle. Recorded by an administrator. */
    private String address;

    /** Driving licence number, e.g. "DL1234567". */
    @Column(nullable = false)
    private String licenseNumber;

    /** The class of vehicle the driver is cleared to operate. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    /** Registration plate, stored uppercased for consistent matching. */
    @Column(nullable = false)
    private String vehicleNumber;

    /** Free-text vehicle details, e.g. "Refrigerated container". */
    @Column(columnDefinition = "TEXT")
    private String vehicleDetails;

    /** Vehicle make, e.g. "Toyota". */
    private String vehicleMake;

    /** Maximum payload the vehicle can carry, in kilograms. */
    private Integer vehicleCapacityKg;

    /** When the driving licence expires; used to warn before it lapses. */
    private LocalDate licenseExpiryDate;

    /** Kind of identification document presented, e.g. "National ID". */
    private String idType;

    /** The identification document number. */
    private String idNumber;

    /**
     * Whether an administrator has checked the licence and national ID. Not the same as the
     * user's email verification, which lives on the user row.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus availabilityStatus;

    /** Required when {@link #availabilityStatus} is {@link DriverStatus#SUSPENDED}. */
    private String suspensionReason;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}