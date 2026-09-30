package com.kilivana.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.admin.dto.*;
import com.kilivana.backend.admin.service.ProfileService;
import com.kilivana.backend.common.dto.ImageResponse;
import com.kilivana.backend.common.enums.UserRole;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.kilivana.backend.support.JwtTestSupport.asBuyer;
import static com.kilivana.backend.support.JwtTestSupport.asDriver;
import static com.kilivana.backend.support.JwtTestSupport.asFarmer;
import static com.kilivana.backend.support.JwtTestSupport.asInspector;
import static com.kilivana.backend.support.JwtTestSupport.asSupplier;
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

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

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

        when(profileService.createFarmerProfile(10L, 10L, request)).thenReturn(response);
        when(profileService.getFarmerProfile(10L, 10L)).thenReturn(response);
        when(profileService.updateFarmerProfile(10L, 10L, request)).thenReturn(response);
        doNothing().when(profileService).deleteFarmerProfile(10L, 10L);

        mockMvc.perform(post("/api/v1/profiles/farmers/10")
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(10));
        mockMvc.perform(get("/api/v1/profiles/farmers/10")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.farmName").value("Green Acres"));
        mockMvc.perform(put("/api/v1/profiles/farmers/10")
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/profiles/farmers/10")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk());
    }

    @Test
    void profileTypes_shouldExposeCreateEndpoints() throws Exception {
        BuyerProfileRequest buyer = BuyerProfileRequest.builder().contactDetails("buyer@example.com").build();
        DriverProfileRequest driver = DriverProfileRequest.builder()
                .licenseNumber("DL-1").vehicleType("Truck").vehicleNumber("KDA 123A").availabilityStatus("AVAILABLE").build();
        InspectorProfileRequest inspector = InspectorProfileRequest.builder().assignedArea("Nakuru").status("ACTIVE").build();
        SupplierProfileRequest supplier = SupplierProfileRequest.builder().businessName("Fresh Foods").location("Nairobi").build();

        when(profileService.createBuyerProfile(11L, 11L, buyer)).thenReturn(BuyerProfileResponse.builder().userId(11L).build());
        when(profileService.createDriverProfile(12L, 12L, driver)).thenReturn(DriverProfileResponse.builder().userId(12L).build());
        when(profileService.createInspectorProfile(13L, 13L, inspector)).thenReturn(InspectorProfileResponse.builder().userId(13L).build());
        when(profileService.createSupplierProfile(14L, 14L, supplier)).thenReturn(SupplierProfileResponse.builder().userId(14L).build());

        mockMvc.perform(post("/api/v1/profiles/buyers/11").with(asUser(jwtService, 11L, UserRole.BUYER)).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(buyer)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/profiles/drivers/12").with(asUser(jwtService, 12L, UserRole.DRIVER)).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(driver)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/profiles/inspectors/13").with(asUser(jwtService, 13L, UserRole.INSPECTOR)).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inspector)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/profiles/suppliers/14").with(asUser(jwtService, 14L, UserRole.SUPPLIER)).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(supplier)))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidFarmerProfile_shouldReturnBadRequest() throws Exception {
        FarmerProfileRequest request = FarmerProfileRequest.builder().location("Nakuru").build();

        mockMvc.perform(post("/api/v1/profiles/farmers/10")
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFarmerImage_shouldReturnImages() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.png", MediaType.IMAGE_PNG_VALUE, "fake-image-data".getBytes());

        List<ImageResponse> images = List.of(ImageResponse.builder()
                .id(1L)
                .url("https://res.cloudinary.com/demo/image/upload/fake.png")
                .publicId("farmer/10/fake")
                .isPrimary(true)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build());

        when(profileService.uploadFarmerProfileImage(10L, 10L, image, null))
                .thenReturn(images);

        mockMvc.perform(multipart("/api/v1/profiles/farmers/10/images")
                        .file(image)
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].isPrimary").value(true));
    }

    @Test
    void getFarmerImages_shouldReturnImages() throws Exception {
        List<ImageResponse> images = List.of(
                ImageResponse.builder().id(1L).url("https://res.cloudinary.com/demo/image/upload/a.png")
                        .publicId("a").isPrimary(true).sortOrder(0).build(),
                ImageResponse.builder().id(2L).url("https://res.cloudinary.com/demo/image/upload/b.png")
                        .publicId("b").isPrimary(false).sortOrder(1).build());

        when(profileService.getFarmerProfileImages(10L, 10L)).thenReturn(images);

        mockMvc.perform(get("/api/v1/profiles/farmers/10/images")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].isPrimary").value(true));
    }

    @Test
    void deleteFarmerImage_shouldReturnSuccess() throws Exception {
        doNothing().when(profileService).deleteFarmerProfileImage(10L, 10L, 1L);

        mockMvc.perform(delete("/api/v1/profiles/farmers/10/images/1")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void setPrimaryFarmerImage_shouldReturnSuccess() throws Exception {
        doNothing().when(profileService).setPrimaryFarmerProfileImage(10L, 10L, 1L);

        mockMvc.perform(put("/api/v1/profiles/farmers/10/images/1/primary")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void uploadSupplierImage_shouldReturnImages() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "supplier.png", MediaType.IMAGE_PNG_VALUE, "fake-data".getBytes());

        List<ImageResponse> images = List.of(ImageResponse.builder()
                .id(1L)
                .url("https://res.cloudinary.com/demo/image/upload/supplier.png")
                .publicId("supplier/10/supplier")
                .isPrimary(true)
                .sortOrder(0)
                .build());

        when(profileService.uploadSupplierProfileImage(10L, 10L, image, null))
                .thenReturn(images);

        mockMvc.perform(multipart("/api/v1/profiles/suppliers/10/images")
                        .file(image)
                        .with(asFarmer(jwtService, 10L))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getSupplierImages_shouldReturnImages() throws Exception {
        List<ImageResponse> images = List.of(ImageResponse.builder()
                .id(1L).url("https://res.cloudinary.com/demo/image/upload/supplier.png")
                .publicId("supplier").isPrimary(true).sortOrder(0).build());

        when(profileService.getSupplierProfileImages(10L, 10L)).thenReturn(images);

        mockMvc.perform(get("/api/v1/profiles/suppliers/10/images")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void deleteSupplierImage_shouldReturnSuccess() throws Exception {
        doNothing().when(profileService).deleteSupplierProfileImage(10L, 10L, 1L);

        mockMvc.perform(delete("/api/v1/profiles/suppliers/10/images/1")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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

        when(profileService.uploadDriverProfileImage(10L, 10L, image, null))
                .thenReturn(images);

        mockMvc.perform(multipart("/api/v1/profiles/drivers/10/images")
                        .file(image)
                        .with(asFarmer(jwtService, 10L))
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
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void deleteDriverImage_shouldReturnSuccess() throws Exception {
        doNothing().when(profileService).deleteDriverProfileImage(10L, 10L, 1L);

        mockMvc.perform(delete("/api/v1/profiles/drivers/10/images/1")
                        .with(asFarmer(jwtService, 10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void imageEndpoints_shouldRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/farmers/10/images"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/profiles/farmers/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void imageEndpoints_shouldRejectMalformedToken() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/farmers/10/images")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void imageEndpoints_shouldIdentifyTheCallerFromTheToken() throws Exception {
        // The acting user must come from the token, not from a client-supplied id.
        List<ImageResponse> images = List.of(ImageResponse.builder()
                .id(1L).url("https://res.cloudinary.com/demo/image/upload/x.png")
                .publicId("x").isPrimary(true).sortOrder(0).build());

        when(profileService.getFarmerProfileImages(10L, 42L)).thenReturn(images);

        mockMvc.perform(get("/api/v1/profiles/farmers/10/images")
                        .with(asFarmer(jwtService, 42L)))
                .andExpect(status().isOk());
    }

    @Test
    void profileCrud_shouldPassTheAuthenticatedCallerNotThePathUserId() throws Exception {
        // A caller acting on someone else's profile must still be described by their
        // own id, so the service can reject them instead of trusting the URL.
        FarmerProfileRequest request = FarmerProfileRequest.builder()
                .farmName("Green Acres")
                .location("Nakuru")
                .build();
        FarmerProfileResponse response = FarmerProfileResponse.builder().id(1L).userId(10L).build();

        when(profileService.getFarmerProfile(42L, 10L)).thenReturn(response);
        when(profileService.updateFarmerProfile(eq(42L), eq(10L), any())).thenReturn(response);
        doNothing().when(profileService).deleteFarmerProfile(42L, 10L);

        mockMvc.perform(get("/api/v1/profiles/farmers/10").with(asFarmer(jwtService, 42L)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profiles/farmers/10")
                        .with(asFarmer(jwtService, 42L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/profiles/farmers/10").with(asFarmer(jwtService, 42L)))
                .andExpect(status().isOk());
    }

    @Test
    void profileCrud_shouldNotRunForAnUnauthorizedCaller() throws Exception {
        // The service refuses this call by throwing, which must surface as 403 and
        // never as a success. Guards against the ownership check being removed.
        FarmerProfileRequest request = FarmerProfileRequest.builder()
                .farmName("Hijacked")
                .location("Nowhere")
                .build();

        when(profileService.getFarmerProfile(42L, 10L))
                .thenThrow(new ForbiddenException("You do not have permission to access this resource"));
        when(profileService.updateFarmerProfile(eq(42L), eq(10L), any()))
                .thenThrow(new ForbiddenException("You do not have permission to access this resource"));
        doThrow(new ForbiddenException("You do not have permission to access this resource"))
                .when(profileService).deleteFarmerProfile(42L, 10L);

        mockMvc.perform(get("/api/v1/profiles/farmers/10").with(asFarmer(jwtService, 42L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/profiles/farmers/10")
                        .with(asFarmer(jwtService, 42L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/profiles/farmers/10").with(asFarmer(jwtService, 42L)))
                .andExpect(status().isForbidden());
    }
}