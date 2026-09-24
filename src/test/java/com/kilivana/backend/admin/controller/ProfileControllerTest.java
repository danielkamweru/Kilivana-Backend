package com.kilivana.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.admin.dto.*;
import com.kilivana.backend.admin.service.ProfileService;
import com.kilivana.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @Test
    void farmerProfile_shouldSupportCrud() throws Exception {
        FarmerProfileRequest request = FarmerProfileRequest.builder()
                .farmName("Green Acres")
                .location("Nakuru")
                .build();
        FarmerProfileResponse response = FarmerProfileResponse.builder()
                .id(1L)
                .userId(10L)
                .farmName("Green Acres")
                .location("Nakuru")
                .build();

        when(profileService.createFarmerProfile(10L, request)).thenReturn(response);
        when(profileService.getFarmerProfile(10L)).thenReturn(response);
        when(profileService.updateFarmerProfile(10L, request)).thenReturn(response);
        doNothing().when(profileService).deleteFarmerProfile(10L);

        mockMvc.perform(post("/api/v1/profiles/farmers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(10));
        mockMvc.perform(get("/api/v1/profiles/farmers/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.farmName").value("Green Acres"));
        mockMvc.perform(put("/api/v1/profiles/farmers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/profiles/farmers/10"))
                .andExpect(status().isOk());
    }

    @Test
    void profileTypes_shouldExposeCreateEndpoints() throws Exception {
        BuyerProfileRequest buyer = BuyerProfileRequest.builder().contactDetails("buyer@example.com").build();
        DriverProfileRequest driver = DriverProfileRequest.builder()
                .licenseNumber("DL-1").vehicleType("Truck").vehicleNumber("KDA 123A").availabilityStatus("AVAILABLE").build();
        InspectorProfileRequest inspector = InspectorProfileRequest.builder().assignedArea("Nakuru").status("ACTIVE").build();
        SupplierProfileRequest supplier = SupplierProfileRequest.builder().businessName("Fresh Foods").location("Nairobi").build();

        when(profileService.createBuyerProfile(11L, buyer)).thenReturn(BuyerProfileResponse.builder().userId(11L).build());
        when(profileService.createDriverProfile(12L, driver)).thenReturn(DriverProfileResponse.builder().userId(12L).build());
        when(profileService.createInspectorProfile(13L, inspector)).thenReturn(InspectorProfileResponse.builder().userId(13L).build());
        when(profileService.createSupplierProfile(14L, supplier)).thenReturn(SupplierProfileResponse.builder().userId(14L).build());

        mockMvc.perform(post("/api/v1/profiles/buyers/11").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(buyer)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/profiles/drivers/12").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(driver)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/profiles/inspectors/13").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inspector)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/profiles/suppliers/14").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(supplier)))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidFarmerProfile_shouldReturnBadRequest() throws Exception {
        FarmerProfileRequest request = FarmerProfileRequest.builder().location("Nakuru").build();

        mockMvc.perform(post("/api/v1/profiles/farmers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}