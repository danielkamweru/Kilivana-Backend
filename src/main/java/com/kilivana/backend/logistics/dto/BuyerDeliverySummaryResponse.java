package com.kilivana.backend.logistics.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class BuyerDeliverySummaryResponse {
    Long orderId;
    String orderStatus;
    List<Delivery> deliveries;

    @Value
    @Builder
    public static class Delivery {
        Long deliveryJobId;
        String status;
        String destinationType;
        LocalDateTime assignedAt;
        LocalDateTime acceptedAt;
        LocalDateTime pickedUpAt;
        LocalDateTime completedAt;
    }
}
