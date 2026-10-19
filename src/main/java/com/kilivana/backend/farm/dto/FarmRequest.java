package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.enums.OwnershipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new farm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a new farm")
public class FarmRequest {

    @NotBlank(message = "Farm name is required")
    @Schema(description = "Name of the farm", example = "Green Valley Farm")
    private String name;

    @NotBlank(message = "County is required")
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

    @NotNull(message = "Farm size is required")
    @Schema(description = "Size of the farm in acres", example = "10.0")
    private Double sizeAcres;

    @NotNull(message = "Ownership type is required")
    @Schema(description = "Ownership type of the farm", example = "LEASED")
    private OwnershipType ownershipType;

    @Schema(description = "Description of the farm", example = "A productive farm growing maize and beans")
    private String description;
}
