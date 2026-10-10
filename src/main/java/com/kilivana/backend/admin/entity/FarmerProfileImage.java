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
 * A photograph attached to a farmer profile — farm shots, certificates, permits. Extends
 * {@link BaseImageEntity} for the shared storage fields and adds only the owner link.
 */
@Entity
@Table(name = "farmer_profile_images", indexes = {
    @Index(name = "idx_farmer_profile_images_user_id", columnList = "userId")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerProfileImage extends BaseImageEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;
}