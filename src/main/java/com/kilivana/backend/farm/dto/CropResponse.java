package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropResponse {
    private Long id;
    private Long farmId;
    private String farmName;
    private Long farmerId;
    private String farmerName;
    private CropTypeSummary cropType;
    private String variety;
    private Double areaAcres;
    private CropStatus status;
    private LocalDate plantingDate;
    private LocalDate expectedHarvestDate;
    private Integer expectedYieldKg;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropTypeSummary {
        private Long id;
        private String name;
        private CropCategory category;
    }
}
