package com.kilivana.backend.common.controller;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicFarmerController.class)
@Import(SecurityConfig.class)
class PublicFarmerControllerTest {

    @MockBean private com.kilivana.backend.security.AccountAccessValidator accountAccessValidator;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getPublicFarmerProfile_validFarmer_shouldReturnProfile() throws Exception {
        User farmer = User.builder()
                .id(23L)
                .name("Jane Farmer")
                .email("jane@example.com")
                .phone("+254700123456")
                .region("Nakuru County")
                .role(UserRole.FARMER)
                .verificationStatus(VerificationStatus.VERIFIED)
                .createdAt(LocalDateTime.of(2026, 10, 7, 10, 0, 0))
                .build();

        when(userRepository.findByIdAndRole(23L, UserRole.FARMER))
                .thenReturn(Optional.of(farmer));

        mockMvc.perform(get("/api/v1/users/farmers/23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(23))
                .andExpect(jsonPath("$.data.name").value("Jane Farmer"))
                .andExpect(jsonPath("$.data.region").value("Nakuru County"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void getPublicFarmerProfile_nonExistentId_shouldReturn404() throws Exception {
        when(userRepository.findByIdAndRole(999L, UserRole.FARMER))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/farmers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getPublicFarmerProfile_nonFarmerRole_shouldReturn404() throws Exception {
        User buyer = User.builder()
                .id(50L)
                .name("John Buyer")
                .email("john@example.com")
                .phone("+254700123457")
                .role(UserRole.BUYER)
                .verificationStatus(VerificationStatus.VERIFIED)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByIdAndRole(50L, UserRole.FARMER))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/farmers/50"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
