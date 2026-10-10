package com.kilivana.backend.farm.entity;

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
 * Represents a crop planted on a specific farm.
 * <p>
 * A crop record ties together a {@link Farm}, the {@link com.kilivana.backend.admin.entity.User farmer}
 * who owns it, and a {@link CropType} catalog entry. The crop tracks its
 * planted area, expected yield, and lifecycle status (PLANTED -> GROWN ->
 * HARVESTED). The {@code status} defaults to PLANNED so a crop can be
 * registered before planting actually occurs.
 * <p>
 * {@code cropTypeId} is stored as a plain identifier rather than a JPA
 * relationship so the crop record remains stable even if the referenced crop
 * type is later renamed or deactivated.
 */
@Entity
@Table(name = "crops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "crop_type_id", nullable = false)
    private Long cropTypeId;

    @Column(nullable = false)
    private String variety;

    @Column(name = "area_acres", nullable = false)
    private Double areaAcres;

    /**
     * Lifecycle state of the crop. Defaults to PLANNED so a crop can be
     * registered before planting actually occurs. Transitions are performed
     * via dedicated status endpoints to keep the lifecycle rules centralized.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.kilivana.backend.common.enums.CropStatus status = com.kilivana.backend.common.enums.CropStatus.PLANNED;

    @Column(name = "planting_date")
    private LocalDate plantingDate;

    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    @Column(name = "expected_yield_kg")
    private Integer expectedYieldKg;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}