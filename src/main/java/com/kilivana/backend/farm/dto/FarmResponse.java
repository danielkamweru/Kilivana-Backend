package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.enums.OwnershipType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a farm, including its images, crops, and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO representing a farm with images, crops, and metadata")
public class FarmResponse {
    @Schema(description = "Unique identifier of the farm", example = "50")
    private Long id;

    @Schema(description = "Identifier of the farmer who owns the farm", example = "12")
    private Long farmerId;

    @Schema(description = "Name of the farm", example = "Green Valley Farm")
    private String name;

    @Schema(description = "County where the farm is located", example = "Nairobi")
    private String county;

    @Schema(description = "Sub-county where the farm is located", example = "Westlands")
    private String subCounty;

    @Schema(description = "Postal address of the farm", example = "P.O. Box 123, Nairobi")
    private String address;

    @Schema(description = "Latitude coordinate of the farm", example = "-1.2921")
    private Double latitude;

    @Schema(description = "Longitude coordinate of the farm", example = "36.8219")
    private Double longitude;

    @Schema(description = "Size of the farm in acres", example = "10.0")
    private Double sizeAcres;

    @Schema(description = "Ownership type of the farm", example = "LEASED")
    private OwnershipType ownershipType;

    @Schema(description = "Current status of the farm", example = "ACTIVE")
    private FarmStatus status;

    @Schema(description = "Description of the farm", example = "A productive farm growing maize and beans")
    private String description;

    @Schema(description = "List of images associated with the farm")
    private List<FarmImageResponse> images;

    @Schema(description = "List of crops grown on the farm")
    private List<CropResponse> crops;

    @Schema(description = "Timestamp when the farm record was created", example = "2024-01-20T09:15:00")
    private LocalDateTime createdAt;
}
