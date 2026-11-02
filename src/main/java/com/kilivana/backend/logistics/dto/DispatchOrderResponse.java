package com.kilivana.backend.logistics.dto;

import com.kilivana.backend.logistics.entity.LogisticsJob;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DispatchOrderResponse {
    Long orderId;
    List<LogisticsJob> deliveryJobs;
}
