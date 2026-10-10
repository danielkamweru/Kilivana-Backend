package com.kilivana.backend.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * The farmer role's profile: farm identity and verification state. One row per user, looked up
 * by {@code userId} — the {@code id} is a surrogate key and the real owner is the user row.
 */
@Entity
@Table(name = "farmer_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The farmer's user id; unique because a user may have at most one farmer profile. */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** Trading name of the farm, e.g. "Green Acres Farm". */
    @Column(nullable = false)
    private String farmName;

    /** Free-text location, e.g. "Kiambu County, near Thika". */
    @Column(nullable = false)
    private String location;

    /** Long description of the farm, its crops and practices. */
    @Column(columnDefinition = "TEXT")
    private String farmDetails;

    /** Evidence of legitimacy: certificates, permit numbers, document references. */
    @Column(columnDefinition = "TEXT")
    private String verificationInfo;

    /** Why the farm is suspended, if it is; null when active. */
    @Column
    private String suspensionReason;

    /**
     * Whether an administrator has approved the farm. Defaults to PENDING so a newly onboarded
     * farmer cannot sell until someone reviews them.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private com.kilivana.backend.common.enums.VerificationStatus kycStatus = com.kilivana.backend.common.enums.VerificationStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
