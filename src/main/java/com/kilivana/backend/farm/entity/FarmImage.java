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
 * Represents an image associated with a farm.
 * <p>
 * A farm can have many images. Exactly one image per farm is marked as the
 * primary image ({@code isPrimary = true}) and is typically used as the
 * farm's thumbnail. The {@code sortOrder} field lets clients control the
 * display order of secondary images. The primary flag and sort order are
 * managed by the application layer; the database does not enforce them.
 */
@Entity
@Table(name = "farm_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(columnDefinition = "TEXT")
    private String url;

    /**
     * Whether this image is the primary image for the farm. Defaults to false;
     * the application is responsible for ensuring at most one image per farm
     * is marked primary.
     */
    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    /**
     * Display order of the image within the farm's image set. Lower values
     * appear first. Defaults to 0.
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}