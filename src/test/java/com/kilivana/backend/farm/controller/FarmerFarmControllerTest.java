package com.kilivana.backend.farm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.common.enums.OwnershipType;
import com.kilivana.backend.farm.dto.FarmRequest;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmImageRepository;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.kilivana.backend.support.JwtTestSupport.asFarmer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FarmerFarmController.class)
@Import(JwtService.class)
class FarmerFarmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FarmRepository farmRepository;

    @MockBean
    private FarmImageRepository farmImageRepository;

    @Test
    void createFarm_existingFarmerWithNoFarm_shouldCreateFarm() throws Exception {
        FarmRequest request = FarmRequest.builder()
                .name("Green Valley Farm")
                .county("Nairobi")
                .subCounty("Westlands")
                .address("P.O. Box 123")
                .latitude(-1.2921)
                .longitude(36.8219)
                .sizeAcres(10.0)
                .ownershipType(OwnershipType.OWNED)
                .description("A productive farm")
                .build();

        Farm savedFarm = Farm.builder()
                .id(1L)
                .farmerId(10L)
                .name("Green Valley Farm")
                .county("Nairobi")
                .subCounty("Westlands")
                .address("P.O. Box 123")
                .latitude(-1.2921)
                .longitude(36.8219)
                .sizeAcres(10.0)
                .ownershipType(OwnershipType.OWNED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .description("A productive farm")
                .createdAt(LocalDateTime.now())
                .build();

        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm);

        mockMvc.perform(post("/api/v1/farmers/me/farms")
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.farmerId").value(10))
                .andExpect(jsonPath("$.data.name").value("Green Valley Farm"))
                .andExpect(jsonPath("$.data.county").value("Nairobi"));
    }

    @Test
    void createFarm_invalidFarmData_shouldReturn400() throws Exception {
        FarmRequest request = FarmRequest.builder()
                .name("") // Invalid: blank name
                .county("") // Invalid: blank county
                .sizeAcres(null) // Invalid: null size
                .ownershipType(null) // Invalid: null ownership type
                .build();

        mockMvc.perform(post("/api/v1/farmers/me/farms")
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyFarm_farmerCanUpdateOwnFarm() throws Exception {
        Farm existingFarm = Farm.builder()
                .id(1L)
                .farmerId(10L)
                .name("Old Name")
                .county("Nairobi")
                .sizeAcres(5.0)
                .ownershipType(OwnershipType.OWNED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        FarmRequest request = FarmRequest.builder()
                .name("Updated Name")
                .county("Kiambu")
                .sizeAcres(15.0)
                .ownershipType(OwnershipType.LEASED)
                .description("Updated description")
                .build();

        Farm updatedFarm = Farm.builder()
                .id(1L)
                .farmerId(10L)
                .name("Updated Name")
                .county("Kiambu")
                .sizeAcres(15.0)
                .ownershipType(OwnershipType.LEASED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .description("Updated description")
                .createdAt(LocalDateTime.now())
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(existingFarm));
        when(farmRepository.save(any(Farm.class))).thenReturn(updatedFarm);

        mockMvc.perform(put("/api/v1/farmers/me/farms/1")
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.county").value("Kiambu"))
                .andExpect(jsonPath("$.data.sizeAcres").value(15.0));
    }

    @Test
    void updateMyFarm_unauthorizedFarmerCannotUpdateAnotherFarm() throws Exception {
        Farm existingFarm = Farm.builder()
                .id(1L)
                .farmerId(99L) // Different farmer
                .name("Other Farm")
                .county("Nairobi")
                .sizeAcres(5.0)
                .ownershipType(OwnershipType.OWNED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        FarmRequest request = FarmRequest.builder()
                .name("Hacked Name")
                .county("Nairobi")
                .sizeAcres(5.0)
                .ownershipType(OwnershipType.OWNED)
                .build();

        when(farmRepository.findById(1L)).thenReturn(Optional.of(existingFarm));

        mockMvc.perform(put("/api/v1/farmers/me/farms/1")
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMyFarms_shouldReturnFarmersFarms() throws Exception {
        Farm farm1 = Farm.builder()
                .id(1L)
                .farmerId(10L)
                .name("Farm 1")
                .county("Nairobi")
                .sizeAcres(10.0)
                .ownershipType(OwnershipType.OWNED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Farm farm2 = Farm.builder()
                .id(2L)
                .farmerId(10L)
                .name("Farm 2")
                .county("Kiambu")
                .sizeAcres(20.0)
                .ownershipType(OwnershipType.LEASED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(farmRepository.findByFarmerId(10L)).thenReturn(Arrays.asList(farm1, farm2));

        mockMvc.perform(get("/api/v1/farmers/me/farms")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Farm 1"))
                .andExpect(jsonPath("$.data[1].name").value("Farm 2"));
    }
}
