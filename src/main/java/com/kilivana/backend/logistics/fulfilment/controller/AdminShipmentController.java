package com.kilivana.backend.logistics.fulfilment.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentCancelRequest;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentCreateRequest;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentItemResponse;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentResponse;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentStopResponse;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentStopsCreateRequest;
import com.kilivana.backend.logistics.fulfilment.dto.ShipmentAssignRequest;
import com.kilivana.backend.logistics.fulfilment.dto.SellerFulfilmentResponse;
import com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment;
import com.kilivana.backend.logistics.fulfilment.entity.Shipment;
import com.kilivana.backend.logistics.fulfilment.entity.ShipmentItem;
import com.kilivana.backend.logistics.fulfilment.entity.ShipmentStop;
import com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus;
import com.kilivana.backend.logistics.fulfilment.enums.StopStatus;
import com.kilivana.backend.logistics.fulfilment.enums.StopType;
import com.kilivana.backend.logistics.fulfilment.repository.SellerFulfilmentRepository;
import com.kilivana.backend.logistics.fulfilment.repository.ShipmentItemRepository;
import com.kilivana.backend.logistics.fulfilment.repository.ShipmentRepository;
import com.kilivana.backend.logistics.fulfilment.repository.ShipmentStopRepository;
import com.kilivana.backend.logistics.fulfilment.service.SellerFulfilmentService;
import com.kilivana.backend.logistics.fulfilment.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-facing shipment management: create shipments from paid orders,
 * assign drivers and vehicles, create route stops, cancel and reassign.
 */
@Tag(name = "Admin Logistics", description = "Admin shipment creation, driver assignment and route management")
@RestController
@RequestMapping("/api/v1/admin/logistics")
@RequiredArgsConstructor
public class AdminShipmentController {

    private final ShipmentService shipmentService;
    private final SellerFulfilmentService fulfilmentService;
    private final ShipmentRepository shipmentRepository;
    private final SellerFulfilmentRepository fulfilmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final ShipmentStopRepository shipmentStopRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Operation(summary = "List all shipments", description = "Returns all shipments (admin only).")
    @GetMapping("/shipments")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> listShipments() {
        List<ShipmentResponse> responses = shipmentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "Get fulfilment records for an order", description = "Returns all seller fulfilment records for an order, used when creating shipments.")
    @GetMapping("/orders/{orderId}/fulfilment")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<List<SellerFulfilmentResponse>>> orderFulfilments(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        List<SellerFulfilmentResponse> responses = fulfilmentService.getFulfilmentsForOrder(orderId).stream()
                .map(this::toFulfilmentResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "Create a shipment", description = "Creates a shipment from a paid order, grouping selected seller fulfilments.")
    @PostMapping("/orders/{orderId}/shipments")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @PathVariable Long orderId,
            @RequestBody ShipmentCreateRequest request) {
        Shipment shipment = shipmentService.createShipment(
                orderId,
                request.getSellerFulfilmentIds(),
                request.getDriverId(),
                request.getVehicleType(),
                request.getVehicleNumber(),
                request.getDriverInstructions());
        return ResponseEntity.ok(ApiResponse.success(toResponse(shipment)));
    }

    @Operation(summary = "Get a shipment by ID", description = "Returns a single shipment with its stops and items.")
    @GetMapping("/shipments/{shipmentId}")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipment(@PathVariable Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
        return ResponseEntity.ok(ApiResponse.success(toResponse(shipment)));
    }

    @Operation(summary = "Assign a driver to a shipment", description = "Assigns a driver and vehicle to a pending shipment.")
    @PostMapping("/shipments/{shipmentId}/assign")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<ShipmentResponse>> assignDriver(
            @PathVariable Long shipmentId,
            @RequestBody ShipmentAssignRequest request) {
        Shipment shipment = shipmentService.assignDriver(shipmentId, request.getDriverId());
        return ResponseEntity.ok(ApiResponse.success(toResponse(shipment)));
    }

    @Operation(summary = "Reassign a shipment", description = "Reassigns a shipment to a different driver, releasing the previous one.")
    @PutMapping("/shipments/{shipmentId}/reassign")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<ShipmentResponse>> reassign(
            @PathVariable Long shipmentId,
            @RequestBody ShipmentAssignRequest request) {
        Shipment shipment = shipmentService.reassign(shipmentId, request.getDriverId());
        return ResponseEntity.ok(ApiResponse.success(toResponse(shipment)));
    }

    @Operation(summary = "Cancel a shipment", description = "Cancels a shipment without cancelling or refunding the original order. Allocations are released.")
    @PutMapping("/shipments/{shipmentId}/cancel")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<ShipmentResponse>> cancel(
            @PathVariable Long shipmentId,
            @RequestBody ShipmentCancelRequest request) {
        Shipment shipment = shipmentService.cancel(shipmentId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(toResponse(shipment)));
    }

    @Operation(summary = "Create route stops", description = "Creates pickup and delivery stops for a shipment.")
    @PostMapping("/shipments/{shipmentId}/stops")
    @PreAuthorize("@resourceAuthorization.admin()")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createStops(
            @PathVariable Long shipmentId,
            @RequestBody ShipmentStopsCreateRequest request) {
        Shipment shipment = shipmentService.createStops(
                shipmentId,
                request.getPickupSellerIds(),
                request.getDeliveryAddress(),
                request.getDeliveryLatitude(),
                request.getDeliveryLongitude());
        return ResponseEntity.ok(ApiResponse.success(toResponse(shipment)));
    }

    private ShipmentResponse toResponse(Shipment s) {
        List<ShipmentStopResponse> stopResponses = s.getShipmentStops() == null
                ? List.of()
                : s.getShipmentStops().stream()
                .sorted(java.util.Comparator.comparing(ShipmentStop::getStopSequence))
                .map(this::toStopResponse)
                .collect(Collectors.toList());

        List<ShipmentItemResponse> itemResponses = s.getShipmentItems() == null
                ? List.of()
                : s.getShipmentItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return ShipmentResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .orderId(s.getOrderId())
                .status(s.getStatus())
                .driverId(s.getDriverId())
                .driverName(driverName)
                .driverPhone(driverPhone)
                .vehicleType(s.getVehicleType())
                .vehicleNumber(s.getVehicleNumber())
                .estimatedWeightKg(s.getEstimatedWeightKg())
                .estimatedVolumeM3(s.getEstimatedVolumeM3())
                .driverInstructions(s.getDriverInstructions())
                .distanceKm(s.getDistanceKm())
                .estimatedMinutes(s.getEstimatedMinutes())
                .assignedAt(s.getAssignedAt())
                .acceptedAt(s.getAcceptedAt())
                .startedAt(s.getStartedAt())
                .completedAt(s.getCompletedAt())
                .cancellationReason(s.getCancellationReason())
                .createdAt(s.getCreatedAt())
                .stops(stopResponses)
                .items(itemResponses)
                .build();
    }

    private ShipmentStopResponse toStopResponse(ShipmentStop stop) {
        return ShipmentStopResponse.builder()
                .id(stop.getId())
                .stopType(stop.getStopType())
                .partyId(stop.getPartyId())
                .address(stop.getAddress())
                .latitude(stop.getLatitude())
                .longitude(stop.getLongitude())
                .stopSequence(stop.getStopSequence())
                .status(stop.getStatus())
                .instructions(stop.getInstructions())
                .arrivedAt(stop.getArrivedAt())
                .completedAt(stop.getCompletedAt())
                .createdAt(stop.getCreatedAt())
                .build();
    }

    private ShipmentItemResponse toItemResponse(ShipmentItem item) {
        OrderItem orderItem = orderItemRepository.findById(item.getOrderItemId()).orElse(null);
        return ShipmentItemResponse.builder()
                .id(item.getId())
                .orderItemId(item.getOrderItemId())
                .productId(orderItem != null ? orderItem.getProductId() : null)
                .productName(orderItem != null ? orderItem.getProductName() : null)
                .allocatedQuantity(item.getAllocatedQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }

    private SellerFulfilmentResponse toFulfilmentResponse(SellerFulfilment f) {
        return SellerFulfilmentResponse.builder()
                .id(f.getId())
                .orderId(f.getOrderId())
                .sellerId(f.getSellerId())
                .sellerSubtotal(f.getSellerSubtotal())
                .status(f.getStatus())
                .notes(f.getNotes())
                .readyAt(f.getReadyAt())
                .collectedAt(f.getCollectedAt())
                .createdAt(f.getCreatedAt())
                .items(List.of())
                .build();
    }
}
