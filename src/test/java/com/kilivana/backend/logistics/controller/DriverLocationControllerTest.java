package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.logistics.service.TrackingAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DriverLocationControllerTest {

    private DriverLocationService driverLocationService;
    private TrackingAuthorizationService authorizationService;
    private DriverLocationController controller;

    @BeforeEach
    void setUp() {
        driverLocationService = mock(DriverLocationService.class);
        authorizationService = mock(TrackingAuthorizationService.class);
        controller = new DriverLocationController(driverLocationService, authorizationService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(17L, null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void statusWithoutARecordedLocationReturnsNotFound() {
        when(driverLocationService.getLatest(24L)).thenReturn(null);

        assertThatThrownBy(() -> controller.getStatus(24L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
