package com.kilivana.backend.farm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropSummaryDto {
    private Long cropTypeId;
    private String name;
    private Long farmCount;
    private Double totalAreaAcres;
    private Long expectedYieldKg;
}
