package com.kilivana.backend.admin.entity;

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
 * A photograph attached to a supplier profile — business licence, facility shots. Extends
 * {@link BaseImageEntity} for the shared storage fields and adds only the owner link.
 */
@Entity
@Table(name = "supplier_profile_images", indexes = {
    @Index(name = "idx_supplier_profile_images_user_id", columnList = "userId")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProfileImage extends BaseImageEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;
}