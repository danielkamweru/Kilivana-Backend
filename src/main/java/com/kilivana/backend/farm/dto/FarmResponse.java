
package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.enums.OwnershipType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO representing a farm, including farmer details,
 * images, crops, and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO representing a farm with farmer details, images, crops, and metadata")
public class FarmResponse {

    @Schema(description = "Unique identifier of the farm", example = "50")
    private Long id;

    @Schema(description = "Details of the farmer who registered this farm")
    private FarmOwnerResponse farmer;

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

    @Schema(description = "Description of the farm",
            example = "A productive farm growing maize and beans")
    private String description;

    @Schema(description = "List of images associated with the farm")
    private List<FarmImageResponse> images = new ArrayList<>();

    @Schema(description = "List of crops grown on the farm")
    private List<CropResponse> crops = new ArrayList<>();

    @Schema(description = "Timestamp when the farm record was created",
            example = "2024-01-20T09:15:00")
    private LocalDateTime createdAt;

    /**
     * Static factory method.
     */
    public static FarmResponse of(
            Long id,
            Long farmerId,
            String name,
            String county,
            String subCounty,
            String address,
            Double latitude,
            Double longitude,
            Double sizeAcres,
            OwnershipType ownershipType,
            FarmStatus status,
            String description,
            List<FarmImageResponse> images,
            List<CropResponse> crops,
            LocalDateTime createdAt) {

        return of(
                id,
                farmerId,
                null,
                name,
                county,
                subCounty,
                address,
                latitude,
                longitude,
                sizeAcres,
                ownershipType,
                status,
                description,
                images,
                crops,
                createdAt
        );
    }

    /**
     * Static factory method including farmer details.
     */
    public static FarmResponse of(
            Long id,
            Long farmerId,
            FarmOwnerResponse farmer,
            String name,
            String county,
            String subCounty,
            String address,
            Double latitude,
            Double longitude,
            Double sizeAcres,
            OwnershipType ownershipType,
            FarmStatus status,
            String description,
            List<FarmImageResponse> images,
            List<CropResponse> crops,
            LocalDateTime createdAt) {

        FarmResponse response = new FarmResponse();

        response.id = id;
        response.farmerId = farmerId;
        response.farmer = farmer;
        response.name = name;
        response.county = county;
        response.subCounty = subCounty;
        response.address = address;
        response.latitude = latitude;
        response.longitude = longitude;
        response.sizeAcres = sizeAcres;
        response.ownershipType = ownershipType;
        response.status = status;
        response.description = description;
        response.images = images != null ? images : new ArrayList<>();
        response.crops = crops != null ? crops : new ArrayList<>();
        response.createdAt = createdAt;

        return response;
    }

    /**
     * Creates the custom builder.
     */
    public static FarmResponseBuilder builder() {
        return new FarmResponseBuilder();
    }

    /**
     * Custom builder for FarmResponse.
     */
    public static class FarmResponseBuilder {

        private Long id;
        private FarmOwnerResponse farmer;
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
        private List<FarmImageResponse> images = new ArrayList<>();
        private List<CropResponse> crops = new ArrayList<>();
        private LocalDateTime createdAt;

        public FarmResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public FarmResponseBuilder farmer(FarmOwnerResponse farmer) {
            this.farmer = farmer;
            return this;
        }

        public FarmResponseBuilder farmerId(Long farmerId) {
            this.farmerId = farmerId;
            return this;
        }

        public FarmResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FarmResponseBuilder county(String county) {
            this.county = county;
            return this;
        }

        public FarmResponseBuilder subCounty(String subCounty) {
            this.subCounty = subCounty;
            return this;
        }

        public FarmResponseBuilder address(String address) {
            this.address = address;
            return this;
        }

        public FarmResponseBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public FarmResponseBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public FarmResponseBuilder sizeAcres(Double sizeAcres) {
            this.sizeAcres = sizeAcres;
            return this;
        }

        public FarmResponseBuilder ownershipType(OwnershipType ownershipType) {
            this.ownershipType = ownershipType;
            return this;
        }

        public FarmResponseBuilder status(FarmStatus status) {
            this.status = status;
            return this;
        }

        public FarmResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public FarmResponseBuilder images(List<FarmImageResponse> images) {
            this.images = images;
            return this;
        }

        public FarmResponseBuilder crops(List<CropResponse> crops) {
            this.crops = crops;
            return this;
        }

        public FarmResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public FarmResponse build() {
            FarmResponse response = new FarmResponse();

            response.id = this.id;
            response.farmer = this.farmer;
            response.farmerId = this.farmerId;
            response.name = this.name;
            response.county = this.county;
            response.subCounty = this.subCounty;
            response.address = this.address;
            response.latitude = this.latitude;
            response.longitude = this.longitude;
            response.sizeAcres = this.sizeAcres;
            response.ownershipType = this.ownershipType;
            response.status = this.status;
            response.description = this.description;
            response.images = this.images != null
                    ? this.images
                    : new ArrayList<>();
            response.crops = this.crops != null
                    ? this.crops
                    : new ArrayList<>();
            response.createdAt = this.createdAt;

            return response;
        }
    }
}
