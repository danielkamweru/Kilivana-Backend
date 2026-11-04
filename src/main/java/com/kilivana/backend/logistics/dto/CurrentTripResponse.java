package com.kilivana.backend.logistics.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import lombok.Value;

@Value
public class CurrentTripResponse {
    Long tripId;

    @JsonUnwrapped
    LogisticsJob job;

    public static CurrentTripResponse from(LogisticsJob job, Long tripId) {
        return new CurrentTripResponse(tripId, job);
    }
}
