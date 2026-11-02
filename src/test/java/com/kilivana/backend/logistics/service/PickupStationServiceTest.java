package com.kilivana.backend.logistics.service;

import com.kilivana.backend.logistics.dto.PickupStationResponse;
import com.kilivana.backend.logistics.entity.PickupStation;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
import com.kilivana.backend.logistics.repository.UserPickupStationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupStationServiceTest {

    @Mock private PickupStationRepository pickupStationRepository;
    @Mock private UserPickupStationRepository userPickupStationRepository;
    @InjectMocks private PickupStationService pickupStationService;

    @Test
    void availableStationsExposeCoordinatesForClientSideDistanceAndMapLinks() {
        PickupStation station = PickupStation.builder()
                .id(12L)
                .name("Westlands Hub")
                .address("Nairobi")
                .region("Nairobi")
                .latitude(-1.2676)
                .longitude(36.8108)
                .isAvailable(true)
                .build();
        when(pickupStationRepository.findByIsAvailableTrue()).thenReturn(List.of(station));

        List<PickupStationResponse> available = pickupStationService.getAvailablePickupStations();

        assertThat(available).hasSize(1);
        assertThat(available.get(0).getLatitude()).isEqualTo(-1.2676);
        assertThat(available.get(0).getLongitude()).isEqualTo(36.8108);
        assertThat(available.get(0).getAddress()).isEqualTo("Nairobi");
        assertThat(available.get(0).getIsAvailable()).isTrue();
    }
}
