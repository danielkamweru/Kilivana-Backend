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

    @Column(nullable = false, unique = true)
    private Long userId;

    /** Where the driver lives and collects the vehicle. Recorded by an administrator. */
    private String address;

    @Column(nullable = false)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private String vehicleNumber;

    @Column(columnDefinition = "TEXT")
    private String vehicleDetails;

    private String vehicleMake;

    /** Maximum payload the vehicle can carry, in kilograms. */
    private Integer vehicleCapacityKg;

    private LocalDate licenseExpiryDate;

    private String idType;
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