package com.kilivana.backend.logistics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.dto.DeliveryTripResponse;
import com.kilivana.backend.logistics.dto.ProofPhotoUploadResponse;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.service.DeliveryProofPhotoService;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.logistics.service.LogisticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripsControllerTest {

    private LogisticsService logisticsService;
    private DriverLocationService driverLocationService;
    private DeliveryProofPhotoService deliveryProofPhotoService;
    private TripsController controller;

    @BeforeEach
    void setUp() {
        logisticsService = mock(LogisticsService.class);
        driverLocationService = mock(DriverLocationService.class);
        deliveryProofPhotoService = mock(DeliveryProofPhotoService.class);
        controller = new TripsController(logisticsService, driverLocationService, deliveryProofPhotoService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(17L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))));
        when(driverLocationService.getActiveStates()).thenReturn(Set.of(
                DeliveryStatus.ACCEPTED, DeliveryStatus.IN_TRANSIT));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeTripsAreLoadedForTheAuthenticatedDriverOnly() {
        LogisticsJob active = LogisticsJob.builder().id(4L).status(DeliveryStatus.IN_TRANSIT).build();
        LogisticsJob assigned = LogisticsJob.builder().id(3L).status(DeliveryStatus.ASSIGNED).build();
        when(logisticsService.getJobsByDriver(17L)).thenReturn(List.of(active, assigned));

        var response = controller.getActiveTrips();

        assertThat(response.getBody().getData()).containsExactly(active);
        verify(logisticsService).getJobsByDriver(17L);
    }

    @Test
    void currentTripReturnsTheMostRecentActiveJob() {
        LogisticsJob newest = LogisticsJob.builder().id(9L).status(DeliveryStatus.ACCEPTED)
                .pickupAddress("Pickup").destinationAddress("Destination").build();
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        when(logisticsService.getJobsByDriver(17L)).thenReturn(List.of(newest));
        when(logisticsService.getTripsForDriver(17L)).thenReturn(List.of(trip));

        var response = controller.getCurrentTrip();

        assertThat(response.getBody().getData().getTripId()).isEqualTo(21L);
        var json = new ObjectMapper().valueToTree(response.getBody().getData());
        assertThat(json.get("tripId").asLong()).isEqualTo(21L);
        assertThat(json.get("id").asLong()).isEqualTo(9L);
        assertThat(json.get("pickupAddress").asText()).isEqualTo("Pickup");
        verify(logisticsService).getTripsForDriver(17L);
    }

    @Test
    void currentTripReturnsNotFoundWhenThereIsNoActiveJob() {
        when(logisticsService.getJobsByDriver(17L)).thenReturn(List.of());
        when(logisticsService.getTripsForDriver(17L)).thenReturn(List.of());

        assertThatThrownBy(() -> controller.getCurrentTrip())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tripLookupUsesTheAuthenticatedDriver() {
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        when(logisticsService.getTripForDriver(21L, 17L)).thenReturn(trip);

        var response = controller.getTrip(21L);

        assertThat(response.getBody().getData().getTripId()).isEqualTo(21L);
        assertThat(new ObjectMapper().valueToTree(response.getBody().getData()).get("tripId").asLong())
                .isEqualTo(21L);
        verify(logisticsService).getTripForDriver(21L, 17L);
    }

    @Test
    void acceptingAJobReturnsTripId() {
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        when(logisticsService.getTripsForDriver(17L)).thenReturn(List.of(trip));

        var response = controller.acceptJob(9L);

        assertThat(response.getBody().getData().getTripId()).isEqualTo(21L);
        assertThat(new ObjectMapper().valueToTree(response.getBody().getData()).get("tripId").asLong())
                .isEqualTo(21L);
        verify(logisticsService).acceptJob(9L, 17L);
    }

    @Test
    void myTripsExposeTripIdInTheResponse() {
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        when(logisticsService.getTripsForDriver(17L)).thenReturn(List.of(trip));

        var response = controller.getMyTrips();

        assertThat(response.getBody().getData())
                .extracting(DeliveryTripResponse::getTripId)
                .containsExactly(21L);
        assertThat(new ObjectMapper().valueToTree(response.getBody().getData()).at("/0/tripId").asLong())
                .isEqualTo(21L);
        verify(logisticsService).getTripsForDriver(17L);
    }

    @Test
    void proofPhotoUploadUsesTheAuthenticatedDriverAndReturnsTheStoredUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "proof.jpg", "image/jpeg", new byte[] {1, 2, 3});
        when(deliveryProofPhotoService.upload(21L, 17L, image))
                .thenReturn(new ProofPhotoUploadResponse("https://api.example/api/v1/images/5"));

        var response = controller.uploadProofPhoto(21L, image);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData().url())
                .isEqualTo("https://api.example/api/v1/images/5");
        verify(deliveryProofPhotoService).upload(21L, 17L, image);
    }

    @Test
    void tripStatusAdvancesItsLinkedDeliveryJob() {
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        LogisticsJob updated = LogisticsJob.builder().id(9L).status(DeliveryStatus.EN_ROUTE_TO_PICKUP).build();
        when(logisticsService.getTripForDriver(21L, 17L)).thenReturn(trip);
        when(logisticsService.updateJobStatus(9L, DeliveryStatus.EN_ROUTE_TO_PICKUP)).thenReturn(updated);

        var response = controller.updateTripStatus(21L, "en_route_to_pickup");

        assertThat(response.getBody().getData()).isSameAs(updated);
        verify(logisticsService).updateJobStatus(9L, DeliveryStatus.EN_ROUTE_TO_PICKUP);
    }

    @Test
    void pickedUpButtonUsesCombinedArrivalAndPickupTransition() {
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        LogisticsJob pickedUp = LogisticsJob.builder().id(9L).status(DeliveryStatus.PICKED_UP).build();
        when(logisticsService.getTripForDriver(21L, 17L)).thenReturn(trip);
        when(logisticsService.markJobPickedUp(9L, 17L)).thenReturn(pickedUp);

        var response = controller.updateTripStatus(21L, "picked_up");

        assertThat(response.getBody().getData()).isSameAs(pickedUp);
        verify(logisticsService).markJobPickedUp(9L, 17L);
    }

    @Test
    void anotherDriversActiveJobCannotStartTracking() {
        DeliveryTrip trip = DeliveryTrip.builder().id(21L).deliveryJobId(9L).driverId(17L).build();
        when(logisticsService.getTripForDriver(21L, 17L)).thenReturn(trip);
        when(logisticsService.getJobById(9L)).thenReturn(LogisticsJob.builder()
                .id(9L).driverId(18L).status(DeliveryStatus.IN_TRANSIT).build());

        assertThatThrownBy(() -> controller.startTracking(21L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void startTrackingResolvesJobThroughTripId() {
        DeliveryTrip trip = DeliveryTrip.builder().id(1L).deliveryJobId(99L).driverId(17L).build();
        LogisticsJob acceptedJob = LogisticsJob.builder().id(99L).driverId(17L)
                .status(DeliveryStatus.ACCEPTED).build();
        when(logisticsService.getTripForDriver(1L, 17L)).thenReturn(trip);
        when(logisticsService.getJobById(99L)).thenReturn(acceptedJob);

        var response = controller.startTracking(1L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(logisticsService).getTripForDriver(1L, 17L);
        verify(logisticsService).getJobById(99L);
        verify(logisticsService).startJobTracking(99L, 17L);
    }

    @Test
    void startTrackingAcceptsLinkedAssignedJobByJobId() {
        DeliveryTrip trip = DeliveryTrip.builder().id(1L).deliveryJobId(99L).driverId(17L).build();
        LogisticsJob assignedJob = LogisticsJob.builder().id(99L).driverId(17L)
                .status(DeliveryStatus.ASSIGNED).build();
        when(logisticsService.getTripForDriver(1L, 17L)).thenReturn(trip);
        when(logisticsService.getJobById(99L)).thenReturn(assignedJob);

        controller.startTracking(1L);

        verify(logisticsService).startJobTracking(99L, 17L);
    }
}
