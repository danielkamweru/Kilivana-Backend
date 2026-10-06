package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.CropCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropTypeDto {
    private Long id;
    private String name;
    private CropCategory category;
    private boolean active;
}
