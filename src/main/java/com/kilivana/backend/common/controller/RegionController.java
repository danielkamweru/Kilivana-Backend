package com.kilivana.backend.common.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.service.KenyaCounty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Reference", description = "Shared reference data the admin panel needs to populate its dropdowns")
@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    /**
     * Public because it is reference data with nothing sensitive in it, and because the
     * sign-in and registration screens need a county list before the caller has a token.
     */
    @GetMapping
    @Operation(summary = "List the Kenyan counties",
            description = "The 47 counties in official order, for the county dropdowns on the farm, "
                    + "farmer and driver forms. Callers should send back one of these values.")
    public ResponseEntity<ApiResponse<List<String>>> listCounties() {
        return ResponseEntity.ok(ApiResponse.success(KenyaCounty.all()));
    }

    @GetMapping("/currency")
    @Operation(summary = "The currency every amount in this API is denominated in")
    public ResponseEntity<ApiResponse<Map<String, String>>> currency() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "code", "KES",
                "symbol", "KSh",
                "label", "Kenyan Shilling")));
    }
}