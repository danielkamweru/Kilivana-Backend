package com.kilivana.backend.logistics.fulfilment.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.logistics.fulfilment.dto.OrderTrackingResponse;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentSummaryResponse;
import com.kilivana.backend.logistics.fulfilment.entity.Shipment;
import com.kilivana.backend.logistics.fulfilment.repository.ShipmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Buyer-facing order tracking: shows one order with its shipments and stops.
 */
@Tag(name = "Buyer Tracking", description = "Buyer order tracking with shipment details")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class BuyerTrackingController {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    @Operation(summary = "Get order tracking", description = "Returns order tracking details: overall status, shipments, stops and driver info.")
    @GetMapping("/{orderId}/tracking")
    @PreAuthorize("@resourceAuthorization.order(#orderId)")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getTracking(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);

        String fulfilmentStatus = aggregateFulfilmentStatus(shipments);

        List<ShipmentSummaryResponse> shipmentResponses = shipments.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        String driverName = null;
        String driverPhone = null;
        LocalDateTime estimatedDelivery = null;
        if (!shipments.isEmpty()) {
            Shipment first = shipments.get(0);
            estimatedDelivery = first.getCompletedAt();
            if (first.getDriverId() != null) {
                driverName = "Driver " + first.getDriverId();
            }
        }

        OrderTrackingResponse response = OrderTrackingResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getCode())
                .orderStatus(order.getStatus() != null ? order.getStatus().name() : null)
                .fulfilmentStatus(fulfilmentStatus)
                .shipments(shipmentResponses)
                .driverName(driverName)
                .driverPhone(driverPhone)
                .estimatedDelivery(estimatedDelivery)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String aggregateFulfilmentStatus(List<Shipment> shipments) {
        if (shipments.isEmpty()) {
            return "PENDING";
        }
        boolean allDelivered = shipments.stream()
                .allMatch(s -> s.getStatus() == com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus.DELIVERED);
        boolean anyFailed = shipments.stream()
                .anyMatch(s -> s.getStatus() == com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus.FAILED);
        boolean anyDelivered = shipments.stream()
                .anyMatch(s -> s.getStatus() == com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus.DELIVERED);

        if (allDelivered) {
            return "COMPLETED";
        }
        if (anyFailed) {
            return "PARTIALLY_FULFILLED";
        }
        if (anyDelivered) {
            return "PARTIALLY_FULFILLED";
        }
        return "PROCESSING";
    }

    private ShipmentSummaryResponse toSummaryResponse(Shipment s) {
        return ShipmentSummaryResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .status(s.getStatus())
                .driverName(s.getDriverId() != null ? "Driver " + s.getDriverId() : null)
                .stops(s.getShipmentStops() == null
                        ? List.of()
                        : s.getShipmentStops().stream()
                        .sorted(java.util.Comparator.comparing(stop -> stop.getStopSequence()))
                        .map(stop -> com.kilivana.backend.logistics.fulfilment.dto.ShipmentStopResponse.builder()
                                .id(stop.getId())
                                .stopType(stop.getStopType())
                                .address(stop.getAddress())
                                .stopSequence(stop.getStopSequence())
                                .status(stop.getStatus())
                                .arrivedAt(stop.getArrivedAt())
                                .completedAt(stop.getCompletedAt())
                                .build())
                        .collect(Collectors.toList()))
                .estimatedDelivery(s.getCompletedAt())
                .build();
    }
}
