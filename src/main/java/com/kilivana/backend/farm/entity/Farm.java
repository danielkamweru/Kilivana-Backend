package com.kilivana.backend.farm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a physical farm owned by a farmer.
 * <p>
 * A farm is the top-level domain concept in the farm module. It aggregates
 * location, size, ownership, and lifecycle state. One farmer can own many
 * farms; each farm in turn hosts one or more {@link Crop} records and one or
 * more {@link FarmImage} records.
 * <p>
 * The {@code status} field drives the farm lifecycle (e.g. whether the farm
 * is visible in public listings and eligible for crop registration). The
 * {@code createdAt} and {@code updatedAt} timestamps are managed by Hibernate
 * and must never be set explicitly by callers.
 */
@Entity
@Table(name = "farms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String county;

    private String subCounty;

    @Column(columnDefinition = "TEXT")
    private String address;

    private Double latitude;
    private Double longitude;

    @Column(name = "size_acres", nullable = false)
    private Double sizeAcres;

    /**
     * Legal arrangement under which the farm is held (owned, leased, etc.).
     * Stored as the enum name so the value is self-describing in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type", nullable = false)
    private com.kilivana.backend.common.enums.OwnershipType ownershipType;

    /**
     * Lifecycle state of the farm. Defaults to ACTIVE so newly registered
     * farms are immediately visible. Status transitions are performed via
     * dedicated endpoints to keep the lifecycle rules centralized.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.kilivana.backend.common.enums.FarmStatus status = com.kilivana.backend.common.enums.FarmStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}