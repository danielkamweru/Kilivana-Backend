package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.enums.OwnershipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmResponse {
    private Long id;
    private Long farmerId;
    private String name;
    private String county;
    private String subCounty;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double sizeAcres;
    private OwnershipType ownershipType;
    private FarmStatus status;
    private String description;
    private List<FarmImageResponse> images;
    private List<CropResponse> crops;
    private LocalDateTime createdAt;
}
