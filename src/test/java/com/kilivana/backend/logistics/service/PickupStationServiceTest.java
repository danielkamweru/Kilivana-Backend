package com.kilivana.backend.logistics.service;

import com.kilivana.backend.logistics.dto.CreatePickupStationRequest;
import com.kilivana.backend.logistics.dto.PickupStationResponse;
import com.kilivana.backend.logistics.entity.PickupStation;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
import com.kilivana.backend.logistics.repository.UserPickupStationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @Test
    void adminCreatedStationPersistsAndReturnsCoordinatesForMaps() {
        CreatePickupStationRequest request = CreatePickupStationRequest.builder()
                .name("Kilivana Westlands Pickup Point")
                .address("Soin Arcade, Westlands, Nairobi, Kenya")
                .latitude(-1.2653443)
                .longitude(36.8039078)
                .region("Nairobi")
                .isAvailable(true)
                .build();
        when(pickupStationRepository.save(any(PickupStation.class))).thenAnswer(invocation -> {
            PickupStation station = invocation.getArgument(0);
            station.setId(8L);
            return station;
        });

        PickupStationResponse response = pickupStationService.createPickupStation(request);

        ArgumentCaptor<PickupStation> stationCaptor = ArgumentCaptor.forClass(PickupStation.class);
        verify(pickupStationRepository).save(stationCaptor.capture());
        assertThat(stationCaptor.getValue().getLatitude()).isEqualTo(-1.2653443);
        assertThat(stationCaptor.getValue().getLongitude()).isEqualTo(36.8039078);
        assertThat(response.getLatitude()).isEqualTo(-1.2653443);
        assertThat(response.getLongitude()).isEqualTo(36.8039078);
    }
}
