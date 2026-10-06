package com.kilivana.backend.admin.entity;

import com.kilivana.backend.common.entity.BaseImageEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inspection_evidence_images", indexes = {
        @Index(name = "idx_inspection_evidence_inspection_id", columnList = "inspectionId")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class InspectionEvidenceImage extends BaseImageEntity {

    @Column(name = "inspection_id", nullable = false)
    private Long inspectionId;
}
