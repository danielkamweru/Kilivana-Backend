package com.kilivana.backend.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An image attached to a product listing.
 *
 * <p>Images are stored via the configured {@link com.kilivana.backend.common.service.ImageStorage}
 * provider (Cloudinary, database or local filesystem). The first uploaded image
 * becomes the primary thumbnail ({@code isPrimary}). {@code sortOrder} controls
 * gallery display order. {@code publicId} and {@code assetId} are the asset
 * manager's identifiers for deletion and replacement.
 */
@Entity
@Table(name = "product_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String url;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
