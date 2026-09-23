package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.logistics.entity.TrackingEvent;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logistics/tracking-events")
@RequiredArgsConstructor
public class TrackingEventController {

    private final LogisticsService logisticsService;

    @PostMapping
    public ResponseEntity<ApiResponse<TrackingEvent>> createTrackingEvent(@RequestBody TrackingEvent event) {
        TrackingEvent created = logisticsService.addTrackingEvent(
                event.getLogisticsJobId(), event.getStatus(), event.getLatitude(),
                event.getLongitude(), event.getNote(), event.getDriverId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackingEvent>> getTrackingEventById(@PathVariable Long id) {
        TrackingEvent event = logisticsService.getTrackingEventById(id);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<TrackingEvent>>> getTrackingHistory(@PathVariable Long jobId) {
        List<TrackingEvent> events = logisticsService.getTrackingHistory(jobId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<TrackingEvent>>> getTrackingEventsByDriver(@PathVariable Long driverId) {
        List<TrackingEvent> events = logisticsService.getTrackingEventsByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
