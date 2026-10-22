
package com.kilivana.backend.farm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Basic details of the farmer who owns a farm")
public class FarmOwnerResponse {

    @Schema(description = "Farmer's user ID", example = "12")
    private Long id;

    @Schema(description = "Farmer's reference code", example = "FARM-0012")
    private String referenceCode;

    @Schema(description = "Farmer's full name", example = "John Mutua")
    private String name;

    @Schema(description = "Farmer's email address", example = "john.mutua@example.com")
    private String email;

    @Schema(description = "Farmer's phone number", example = "+254700123456")
    private String phone;

    @Schema(description = "County where the farmer is located", example = "Nairobi")
    private String county;
}
