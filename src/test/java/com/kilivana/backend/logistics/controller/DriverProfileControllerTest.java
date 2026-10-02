package com.kilivana.backend.logistics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.admin.dto.DriverProfileRequest;
import com.kilivana.backend.admin.dto.DriverProfileResponse;
import com.kilivana.backend.admin.service.ProfileService;
import com.kilivana.backend.common.dto.ImageResponse;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.VehicleType;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.security.JwtService;
import com.kilivana.backend.support.JwtTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.kilivana.backend.support.JwtTestSupport.asBuyer;
import static com.kilivana.backend.support.JwtTestSupport.asDriver;
import static com.kilivana.backend.support.JwtTestSupport.asUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Driver endpoints are grouped under Logistics, so they are covered separately from the
 * administration-side role profiles.
 */
@WebMvcTest(DriverProfileController.class)
@Import(SecurityConfig.class)
class DriverProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProfileService profileService;

    @Test
    void createDriverProfile_shouldReturnCreated() throws Exception {
        DriverProfileRequest request = DriverProfileRequest.builder()
                .licenseNumber("DL-1")
                .vehicleType(VehicleType.TRUCK)
                .vehicleNumber("KDA 123A")
                // The capacity as an administrator writes it, not kilograms.
                .vehicleCapacity("5T")
                .availabilityStatus(DriverStatus.AVAILABLE)
                .build();

        when(profileService.createDriverProfile(eq(12L), eq(12L), any()))
                .thenReturn(DriverProfileResponse.builder().userId(12L).build());

        mockMvc.perform(post("/api/v1/profiles/drivers/12")
                        .with(asUser(jwtService, 12L, UserRole.DRIVER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(12));
    }

    @Test
    void uploadDriverImage_shouldReturnImages() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "driver.png", MediaType.IMAGE_PNG_VALUE, "fake-data".getBytes());

        List<ImageResponse> images = List.of(ImageResponse.builder()
                .id(1L)
                .url("https://res.cloudinary.com/demo/image/upload/driver.png")
                .publicId("driver/10/driver")
                .isPrimary(true)
                .sortOrder(0)
                .build());

        when(profileService.uploadDriverProfileImage(10L, 10L, image, null)).thenReturn(images);

        mockMvc.perform(multipart("/api/v1/profiles/drivers/10/images")
                        .file(image)
                        .with(asDriver(jwtService, 10L))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getDriverImages_shouldReturnImages() throws Exception {
        List<ImageResponse> images = List.of(ImageResponse.builder()
                .id(1L).url("https://res.cloudinary.com/demo/image/upload/driver.png")
                .publicId("driver").isPrimary(true).sortOrder(0).build());

        when(profileService.getDriverProfileImages(10L, 10L)).thenReturn(images);

        mockMvc.perform(get("/api/v1/profiles/drivers/10/images")
                        .with(asDriver(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void deleteDriverImage_shouldReturnSuccess() throws Exception {
        doNothing().when(profileService).deleteDriverProfileImage(10L, 10L, 1L);

        mockMvc.perform(delete("/api/v1/profiles/drivers/10/images/1")
                        .with(asDriver(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void driverEndpoints_shouldPassTheAuthenticatedCaller() throws Exception {
        // The caller's own id, not the path id, must reach the service.
        DriverProfileResponse response = DriverProfileResponse.builder().userId(10L).build();
        when(profileService.getDriverProfile(99L, 10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/profiles/drivers/10").with(asDriver(jwtService, 99L)))
                .andExpect(status().isOk());
    }

    @Test
    void driverEndpoints_shouldRejectAnUnauthorizedCaller() throws Exception {
        // A buyer must not be able to read or rewrite a driver's licence details.
        when(profileService.getDriverProfile(4L, 10L))
                .thenThrow(new ForbiddenException("You do not have permission to access this resource"));
        when(profileService.updateDriverProfile(eq(4L), eq(10L), any()))
                .thenThrow(new ForbiddenException("You do not have permission to access this resource"));
        doThrow(new ForbiddenException("You do not have permission to access this resource"))
                .when(profileService).deleteDriverProfile(4L, 10L);
        doThrow(new ForbiddenException("You do not have permission to access this resource"))
                .when(profileService).deleteDriverProfileImage(4L, 10L, 1L);

        DriverProfileRequest request = DriverProfileRequest.builder()
                .licenseNumber("STOLEN")
                .vehicleType(VehicleType.TRUCK)
                .vehicleNumber("KDA 123A")
                .availabilityStatus(DriverStatus.AVAILABLE)
                .build();

        mockMvc.perform(get("/api/v1/profiles/drivers/10").with(asBuyer(jwtService, 4L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/profiles/drivers/10")
                        .with(asBuyer(jwtService, 4L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/profiles/drivers/10").with(asBuyer(jwtService, 4L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/profiles/drivers/10/images/1").with(asBuyer(jwtService, 4L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createDriverProfile_shouldAcceptLowerCaseAndHyphenatedEnums() throws Exception {
        // The admin panel sends "available" and "on-delivery"; both have to be understood or
        // the client is pushed into inventing its own mapping, which is where a second spelling
        // of the same state comes from.
        String body = """
                {
                  "licenseNumber": "DL-9",
                  "vehicleType": "van",
                  "vehicleNumber": "KDA 999Z",
                  "vehicleCapacity": "200kg",
                  "kycStatus": "verified",
                  "availabilityStatus": "on-delivery"
                }
                """;

        when(profileService.createDriverProfile(eq(12L), eq(12L), any()))
                .thenReturn(DriverProfileResponse.builder()
                        .userId(12L)
                        .vehicleType(VehicleType.VAN)
                        .vehicleCapacityKg(200)
                        .kycStatus(KycStatus.VERIFIED)
                        .availabilityStatus(DriverStatus.ON_DELIVERY)
                        .build());

        mockMvc.perform(post("/api/v1/profiles/drivers/12")
                        .with(asUser(jwtService, 12L, UserRole.DRIVER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.vehicleType").value("VAN"))
                .andExpect(jsonPath("$.data.availabilityStatus").value("ON_DELIVERY"))
                .andExpect(jsonPath("$.data.kycStatus").value("VERIFIED"));
    }

    @Test
    void createDriverProfile_shouldRejectAnUnknownVehicleType() throws Exception {
        String body = """
                {
                  "licenseNumber": "DL-9",
                  "vehicleType": "Bicycle",
                  "vehicleNumber": "KDA 999Z",
                  "availabilityStatus": "available"
                }
                """;

        mockMvc.perform(post("/api/v1/profiles/drivers/12")
                        .with(asUser(jwtService, 12L, UserRole.DRIVER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void driverEndpoints_shouldRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/drivers/10"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/profiles/drivers/10/images"))
                .andExpect(status().isUnauthorized());
    }
}
