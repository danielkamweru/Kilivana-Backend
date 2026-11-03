package com.kilivana.backend.logistics.dto;

import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.enums.DeliveryTripStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class DeliveryTripResponse {
    Long tripId;
    Long deliveryJobId;
    Long driverId;
    DeliveryTripStatus status;
    LocalDateTime startedAt;
    LocalDateTime pickedUpAt;
    LocalDateTime completedAt;
    Double lastLatitude;
    Double lastLongitude;
    LocalDateTime lastLocationUpdatedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static DeliveryTripResponse fromEntity(DeliveryTrip trip) {
        return DeliveryTripResponse.builder()
                .tripId(trip.getId())
                .deliveryJobId(trip.getDeliveryJobId())
                .driverId(trip.getDriverId())
                .status(trip.getStatus())
                .startedAt(trip.getStartedAt())
                .pickedUpAt(trip.getPickedUpAt())
                .completedAt(trip.getCompletedAt())
                .lastLatitude(trip.getLastLatitude())
                .lastLongitude(trip.getLastLongitude())
                .lastLocationUpdatedAt(trip.getLastLocationUpdatedAt())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}
