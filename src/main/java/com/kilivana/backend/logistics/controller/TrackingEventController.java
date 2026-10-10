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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Delivery tracking: recording tracking events and reading tracking
 * history for a delivery job or a driver.
 *
 * <p>Tracking events are the low-frequency status history (one per status change, with
 * an optional position), distinct from the high-frequency position stream served by
 * {@code DriverLocationController}. A client wanting to redraw a journey reads the
 * tracking events; a client wanting live animation subscribes to the WebSocket topic.
 */
@Tag(name = "Logistics · Tracking", description = "Delivery tracking events and history")
@RestController
@RequestMapping("/api/v1/logistics/tracking-events")
@RequiredArgsConstructor
public class TrackingEventController {

    private final LogisticsService logisticsService;

    @Operation(summary = "Record a tracking event",
            description = "Records a tracking event (status, position and optional note) for a delivery job.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tracking event recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TrackingEvent>> createTrackingEvent(@RequestBody TrackingEvent event) {
        TrackingEvent created = logisticsService.addTrackingEvent(
                event.getLogisticsJobId(), event.getStatus(), event.getLatitude(),
                event.getLongitude(), event.getNote(), event.getDriverId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "Get a tracking event by ID", description = "Returns a single tracking event.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking event found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tracking event not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackingEvent>> getTrackingEventById(
            @Parameter(description = "ID of the tracking event") @PathVariable Long id) {
        TrackingEvent event = logisticsService.getTrackingEventById(id);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @Operation(summary = "Get tracking history of a job",
            description = "Returns the tracking events of one delivery job, most recent first.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking history returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<TrackingEvent>>> getTrackingHistory(
            @Parameter(description = "ID of the delivery job") @PathVariable Long jobId) {
        List<TrackingEvent> events = logisticsService.getTrackingHistory(jobId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @Operation(summary = "List tracking events of a driver",
            description = "Returns the tracking events recorded by one driver.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking events returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<TrackingEvent>>> getTrackingEventsByDriver(
            @Parameter(description = "ID of the driver") @PathVariable Long driverId) {
        List<TrackingEvent> events = logisticsService.getTrackingEventsByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
