package com.kilivana.backend.farm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a catalog entry describing a type of crop (e.g. Maize, Beans).
 * <p>
 * Crop types are reference data used when registering {@link Crop} records.
 * The {@code name} must be unique so that dropdowns and aggregations can
 * reliably map a crop record back to its type. The {@code active} flag lets
 * the catalog be curated: deactivated types are hidden from dropdowns but
 * remain attached to historical crop records.
 */
@Entity
@Table(name = "crop_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Broad classification of the crop (e.g. CEREAL, ROOT_CROP). Stored as the
     * enum name so the value is self-describing in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.kilivana.backend.common.enums.CropCategory category;

    /**
     * Whether the crop type is available for use in new crop registrations.
     * Defaults to true. Deactivating a type hides it from dropdowns without
     * deleting it, preserving historical crop records.
     */
    @Column(nullable = false)
    private boolean active = true;
}