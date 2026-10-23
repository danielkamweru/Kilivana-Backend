package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.entity.FarmerProfileImage;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.FarmerProfileImageRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.admin.service.ProfileService;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.entity.FarmImage;
import com.kilivana.backend.farm.repository.FarmImageRepository;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.security.AccountAccessValidator;
import com.kilivana.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.kilivana.backend.support.JwtTestSupport.asAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminFarmerController.class)
@Import(com.kilivana.backend.config.SecurityConfig.class)
class AdminFarmerControllerTest {
    @MockBean
    private AccountAccessValidator accountAccessValidator;
    @MockBean
    private UserService userService;
    @MockBean
    private ProfileService profileService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FarmRepository farmRepository;

    @MockBean
    private FarmImageRepository farmImageRepository;

    @MockBean
    private FarmerProfileImageRepository farmerProfileImageRepository;

    private User farmer;
    private FarmerProfileImage profileImage;

    @BeforeEach
    void setUp() {
        farmer = User.builder()
                .id(10L)
                .name("John Farmer")
                .email("john@example.com")
                .phone("+254700123456")
                .username("johnfarmer")
                .region("Nairobi")
                .role(UserRole.FARMER)
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        profileImage = FarmerProfileImage.builder()
                .id(1L)
                .userId(10L)
                .url("https://example.com/profile.jpg")
                .publicId("profile-1")
                .assetId("asset-1")
                .sortOrder(0)
                .isPrimary(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getFarmer_withProfileImagesButNoFarms_shouldReturnProfileImages() throws Exception {
        when(userRepository.findByIdAndRole(10L, UserRole.FARMER))
                .thenReturn(Optional.of(farmer));
        when(farmRepository.findByFarmerId(10L))
                .thenReturn(Collections.emptyList());
        when(farmerProfileImageRepository.findByUserIdOrderBySortOrderAsc(10L))
                .thenReturn(Collections.singletonList(profileImage));

        mockMvc.perform(get("/api/v1/admin/farmers/10")
                        .with(asAdmin(jwtService, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.profileImages").isArray())
                .andExpect(jsonPath("$.data.profileImages[0].id").value(1))
                .andExpect(jsonPath("$.data.profileImages[0].url").value("https://example.com/profile.jpg"))
                .andExpect(jsonPath("$.data.farms").isArray())
                .andExpect(jsonPath("$.data.farms").isEmpty());
    }

    @Test
    void getFarmer_withFarmImagesButNoProfileImages_shouldReturnFarmImages() throws Exception {
        Farm farm = Farm.builder()
                .id(1L)
                .farmerId(10L)
                .name("Green Farm")
                .county("Nairobi")
                .sizeAcres(10.0)
                .ownershipType(com.kilivana.backend.common.enums.OwnershipType.OWNED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        FarmImage farmImage = FarmImage.builder()
                .id(100L)
                .farmId(1L)
                .url("https://example.com/farm.jpg")
                .isPrimary(true)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByIdAndRole(10L, UserRole.FARMER))
                .thenReturn(Optional.of(farmer));
        when(farmRepository.findByFarmerId(10L))
                .thenReturn(Collections.singletonList(farm));
        when(farmImageRepository.findByFarmIdOrderBySortOrderAsc(1L))
                .thenReturn(Collections.singletonList(farmImage));
        when(farmerProfileImageRepository.findByUserIdOrderBySortOrderAsc(10L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/farmers/10")
                        .with(asAdmin(jwtService, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.profileImages").isArray())
                .andExpect(jsonPath("$.data.profileImages").isEmpty())
                .andExpect(jsonPath("$.data.farms").isArray())
                .andExpect(jsonPath("$.data.farms[0].images").isArray())
                .andExpect(jsonPath("$.data.farms[0].images[0].id").value(100))
                .andExpect(jsonPath("$.data.farms[0].images[0].url").value("https://example.com/farm.jpg"));
    }

    @Test
    void getFarmer_withBothProfileAndFarmImages_shouldReturnBoth() throws Exception {
        Farm farm = Farm.builder()
                .id(1L)
                .farmerId(10L)
                .name("Green Farm")
                .county("Nairobi")
                .sizeAcres(10.0)
                .ownershipType(com.kilivana.backend.common.enums.OwnershipType.OWNED)
                .status(com.kilivana.backend.common.enums.FarmStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        FarmImage farmImage = FarmImage.builder()
                .id(100L)
                .farmId(1L)
                .url("https://example.com/farm.jpg")
                .isPrimary(true)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByIdAndRole(10L, UserRole.FARMER))
                .thenReturn(Optional.of(farmer));
        when(farmRepository.findByFarmerId(10L))
                .thenReturn(Collections.singletonList(farm));
        when(farmImageRepository.findByFarmIdOrderBySortOrderAsc(1L))
                .thenReturn(Collections.singletonList(farmImage));
        when(farmerProfileImageRepository.findByUserIdOrderBySortOrderAsc(10L))
                .thenReturn(Collections.singletonList(profileImage));

        mockMvc.perform(get("/api/v1/admin/farmers/10")
                        .with(asAdmin(jwtService, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.profileImages").isArray())
                .andExpect(jsonPath("$.data.profileImages[0].id").value(1))
                .andExpect(jsonPath("$.data.farms").isArray())
                .andExpect(jsonPath("$.data.farms[0].images").isArray())
                .andExpect(jsonPath("$.data.farms[0].images[0].id").value(100));
    }

    @Test
    void getFarmer_withNeitherProfileNorFarmImages_shouldReturnEmptyLists() throws Exception {
        when(userRepository.findByIdAndRole(10L, UserRole.FARMER))
                .thenReturn(Optional.of(farmer));
        when(farmRepository.findByFarmerId(10L))
                .thenReturn(Collections.emptyList());
        when(farmerProfileImageRepository.findByUserIdOrderBySortOrderAsc(10L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/farmers/10")
                        .with(asAdmin(jwtService, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.profileImages").isArray())
                .andExpect(jsonPath("$.data.profileImages").isEmpty())
                .andExpect(jsonPath("$.data.farms").isArray())
                .andExpect(jsonPath("$.data.farms").isEmpty());
    }

    @Test
    void getFarmer_nonExistentId_shouldReturn404() throws Exception {
        when(userRepository.findByIdAndRole(999L, UserRole.FARMER))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/farmers/999")
                        .with(asAdmin(jwtService, 1L)))
                .andExpect(status().isNotFound());
    }
}
