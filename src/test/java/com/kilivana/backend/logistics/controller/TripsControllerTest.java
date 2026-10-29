package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.logistics.service.LogisticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
    private TripsController controller;

    @BeforeEach
    void setUp() {
        logisticsService = mock(LogisticsService.class);
        driverLocationService = mock(DriverLocationService.class);
        controller = new TripsController(logisticsService, driverLocationService);
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
        LogisticsJob newest = LogisticsJob.builder().id(9L).status(DeliveryStatus.ACCEPTED).build();
        when(logisticsService.getJobsByDriver(17L)).thenReturn(List.of(newest));

        var response = controller.getCurrentTrip();

        assertThat(response.getBody().getData()).isSameAs(newest);
    }

    @Test
    void currentTripReturnsNotFoundWhenThereIsNoActiveJob() {
        when(logisticsService.getJobsByDriver(17L)).thenReturn(List.of());

        assertThatThrownBy(() -> controller.getCurrentTrip())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anotherDriversActiveJobCannotStartTracking() {
        when(logisticsService.getJobById(9L)).thenReturn(LogisticsJob.builder()
                .id(9L).driverId(18L).status(DeliveryStatus.IN_TRANSIT).build());

        assertThatThrownBy(() -> controller.startTracking(9L))
                .isInstanceOf(ForbiddenException.class);
    }
}
