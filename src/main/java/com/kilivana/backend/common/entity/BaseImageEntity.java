package com.kilivana.backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Fields every image-bearing entity shares, so galleries of products, drivers, farms
 * and the like are stored and served uniformly.
 */
@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable, client-facing identifier; unique across the image's table. */
    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    /** The storage provider's own id (e.g. Cloudinary's); {@code null} when the provider has none. */
    @Column(name = "asset_id")
    private String assetId;

    /** Public URL the image bytes can be fetched from. */
    @Column(name = "url", nullable = false)
    private String url;

    /** The entity this image belongs to, e.g. a product id; joins the gallery to its owner. */
    @Column(name = "resource_id")
    private Long resourceId;

    /** Display order within the owning entity's gallery. */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** Whether this image is the entity's cover image. */
    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}