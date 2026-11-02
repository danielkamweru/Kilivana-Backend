package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.service.LogisticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LogisticsJobControllerTest {

    private LogisticsService logisticsService;
    private LogisticsJobController controller;

    @BeforeEach
    void setUp() {
        logisticsService = mock(LogisticsService.class);
        controller = new LogisticsJobController(logisticsService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administratorsReceiveAllDeliveryJobs() {
        LogisticsJob job = LogisticsJob.builder().id(1L).build();
        when(logisticsService.getAllJobs()).thenReturn(List.of(job));
        authenticate(1L, "ADMIN");

        var response = controller.getAllJobs(Pageable.unpaged());

        assertThat(response.getBody().getData()).containsExactly(job);
        verify(logisticsService).getAllJobs();
    }

    @Test
    void driversReceiveOnlyJobsAssignedToTheirAccount() {
        LogisticsJob job = LogisticsJob.builder().id(2L).driverId(17L).build();
        when(logisticsService.getJobsByDriver(17L)).thenReturn(List.of(job));
        authenticate(17L, "DRIVER");

        var response = controller.getAllJobs(Pageable.unpaged());

        assertThat(response.getBody().getData()).containsExactly(job);
        verify(logisticsService).getJobsByDriver(17L);
    }

    @Test
    void otherRolesCannotListDeliveryJobs() {
        authenticate(22L, "BUYER");

        assertThatThrownBy(() -> controller.getAllJobs(Pageable.unpaged()))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(logisticsService);
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
