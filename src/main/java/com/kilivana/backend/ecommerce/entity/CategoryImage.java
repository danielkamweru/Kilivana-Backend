package com.kilivana.backend.ecommerce.entity;

import com.kilivana.backend.common.entity.BaseImageEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * An image attached to a product category, inheriting common image fields
 * (URL, asset manager IDs, sort order, primary flag) from {@link BaseImageEntity}.
 */
@Entity
@Table(name = "category_images", indexes = {
    @Index(name = "idx_category_images_category_id", columnList = "categoryId")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryImage extends BaseImageEntity {

    @Column(name = "category_id", nullable = false)
    private Long categoryId;
}