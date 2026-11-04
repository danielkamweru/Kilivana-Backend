package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryProofPhotoServiceTest {

    private LogisticsService logisticsService;
    private ImageStorage imageStorage;
    private DeliveryProofPhotoService service;

    @BeforeEach
    void setUp() {
        logisticsService = mock(LogisticsService.class);
        imageStorage = mock(ImageStorage.class);
        service = new DeliveryProofPhotoService(logisticsService, imageStorage);
    }

    @Test
    void uploadsPhotoForAssignedActiveDeliveryAndReturnsItsUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "proof.jpg", "image/jpeg", new byte[] {1, 2, 3});
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        LogisticsJob job = LogisticsJob.builder().id(9L).status(DeliveryStatus.IN_TRANSIT).build();
        when(logisticsService.getTripForDriver(21L, 17L)).thenReturn(trip);
        when(logisticsService.getJobById(9L)).thenReturn(job);
        when(imageStorage.uploadImage(image, "kilivana/delivery-proof/9"))
                .thenReturn(Map.of("secure_url", "https://api.example/api/v1/images/5"));
        when(imageStorage.getSecureUrl(any())).thenReturn("https://api.example/api/v1/images/5");

        var response = service.upload(21L, 17L, image);

        assertThat(response.url()).isEqualTo("https://api.example/api/v1/images/5");
        verify(imageStorage).uploadImage(image, "kilivana/delivery-proof/9");
    }

    @Test
    void doesNotUploadPhotoForJobThatIsNotReadyForProof() throws Exception {
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        when(logisticsService.getTripForDriver(21L, 17L)).thenReturn(trip);
        when(logisticsService.getJobById(9L)).thenReturn(LogisticsJob.builder()
                .id(9L).status(DeliveryStatus.PICKED_UP).build());

        assertThatThrownBy(() -> service.upload(21L, 17L,
                new MockMultipartFile("image", "proof.jpg", "image/jpeg", new byte[] {1})))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active delivery");
        verify(imageStorage, never()).uploadImage(any(), any());
    }

    @Test
    void refusesPhotoUploadForAnotherDriversTrip() throws Exception {
        when(logisticsService.getTripForDriver(21L, 17L))
                .thenThrow(new ForbiddenException("Only the assigned driver can access this trip"));

        assertThatThrownBy(() -> service.upload(21L, 17L,
                new MockMultipartFile("image", "proof.jpg", "image/jpeg", new byte[] {1})))
                .isInstanceOf(ForbiddenException.class);
        verify(imageStorage, never()).uploadImage(any(), any());
    }
}
