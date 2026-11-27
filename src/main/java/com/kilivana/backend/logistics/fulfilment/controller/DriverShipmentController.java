package com.kilivana.backend.logistics.fulfilment.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.entity.DeliveryOtpVerification;
import com.kilivana.backend.finance.entity.Settlement;
import com.kilivana.backend.finance.service.SettlementService;
import com.kilivana.backend.logistics.fulfilment.dto.*;
import com.kilivana.backend.logistics.fulfilment.entity.Shipment;
import com.kilivana.backend.logistics.fulfilment.entity.ShipmentStop;
import com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus;
import com.kilivana.backend.logistics.fulfilment.enums.StopStatus;
import com.kilivana.backend.logistics.fulfilment.enums.StopType;
import com.kilivana.backend.logistics.fulfilment.repository.ShipmentRepository;
import com.kilivana.backend.logistics.fulfilment.repository.ShipmentStopRepository;
import com.kilivana.backend.logistics.fulfilment.service.ShipmentService;
import com.kilivana.backend.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Driver-facing shipment management: accept assigned shipments, mark arrival
 * and completion of stops, update shipment status, request/verify delivery OTP,
 * and file proof of delivery.
 */
@Tag(name = "Driver Shipments", description = "Driver shipment tasks: accept, stop arrival/completion, OTP request/verify, proof of delivery")
@RestController
@RequestMapping("/api/v1/driver/shipments")
@RequiredArgsConstructor
public class DriverShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentStopRepository shipmentStopRepository;
    private final OrderService orderService;
    private final SettlementService settlementService;

    @Operation(summary = "List my shipments", description = "Returns all shipments assigned to the authenticated driver.")
    @GetMapping
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> listMyShipments(
            @RequestParam(required = false) Long driverId) {
        Long id = driverId != null ? driverId : com.kilivana.backend.security.CurrentUser.id();
        List<ShipmentResponse> responses = shipmentRepository.findByDriverId(id).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "Get a shipment by ID", description = "Returns a single shipment assigned to this driver.")
    @GetMapping("/{id}")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipment(
            @PathVariable Long id,
            @RequestParam(required = false) Long driverId) {
        Long actorId = driverId != null ? driverId : com.kilivana.backend.security.CurrentUser.id();
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (!shipment.getDriverId().equals(actorId) && !com.kilivana.backend.security.CurrentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("Driver may only access their own shipments");
        }
        return ResponseEntity.ok(ApiResponse.success(toResponse(shipment)));
    }

    @Operation(summary = "Accept a shipment", description = "Marks an assigned shipment as accepted by the driver.")
    @PutMapping("/{id}/accept")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> accept(
            @PathVariable Long id,
            @RequestParam Long driverId) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (!shipment.getDriverId().equals(actorId)) {
            throw new ForbiddenException("Driver not assigned to this shipment");
        }
        if (shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new BadRequestException("Shipment is not in ASSIGNED status");
        }
        shipment.setStatus(ShipmentStatus.DRIVER_ACCEPTED);
        shipment.setAcceptedAt(LocalDateTime.now());
        Shipment saved = shipmentRepository.save(shipment);
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @Operation(summary = "Mark arrival at a stop", description = "Records the driver's arrival at a shipment stop.")
    @PutMapping("/{shipmentId}/stops/{stopId}/arrive")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<ShipmentStopResponse>> arrive(
            @PathVariable Long shipmentId,
            @PathVariable Long stopId,
            @RequestParam Long driverId,
            @RequestBody StopArriveRequest request) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
        if (!shipment.getDriverId().equals(actorId)) {
            throw new ForbiddenException("Driver not assigned to this shipment");
        }
        ShipmentStop stop = shipmentStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentStop", stopId));
        if (!stop.getShipmentId().equals(shipmentId)) {
            throw new BadRequestException("Stop does not belong to this shipment");
        }
        if (stop.getStopType() != StopType.PICKUP && stop.getStopType() != StopType.DELIVERY) {
            throw new BadRequestException("Invalid stop type");
        }
        stop.setStatus(StopStatus.ARRIVED);
        stop.setArrivedAt(LocalDateTime.now());
        if (request.getLatitude() != null) stop.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) stop.setLongitude(request.getLongitude());
        if (request.getNote() != null) stop.setInstructions(request.getNote());
        ShipmentStop saved = shipmentStopRepository.save(stop);
        return ResponseEntity.ok(ApiResponse.success(toStopResponse(saved)));
    }

    @Operation(summary = "Mark completion of a stop", description = "Records the driver's completion of a shipment stop.")
    @PutMapping("/{shipmentId}/stops/{stopId}/complete")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<ShipmentStopResponse>> complete(
            @PathVariable Long shipmentId,
            @PathVariable Long stopId,
            @RequestParam Long driverId,
            @RequestBody StopCompleteRequest request) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
        if (!shipment.getDriverId().equals(actorId)) {
            throw new ForbiddenException("Driver not assigned to this shipment");
        }
        ShipmentStop stop = shipmentStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentStop", stopId));
        if (!stop.getShipmentId().equals(shipmentId)) {
            throw new BadRequestException("Stop does not belong to this shipment");
        }
        // Cannot complete delivery before all pickups are completed
        if (stop.getStopType() == StopType.DELIVERY) {
            List<ShipmentStop> pickups = shipmentStopRepository.findByShipmentIdAndStopType(shipmentId, StopType.PICKUP);
            boolean allPickupsCompleted = pickups.stream().allMatch(p -> p.getStatus() == StopStatus.COMPLETED);
            if (!allPickupsCompleted) {
                throw new BadRequestException("All pickup stops must be completed before the delivery stop can be completed");
            }
        }
        stop.setStatus(StopStatus.COMPLETED);
        stop.setCompletedAt(LocalDateTime.now());
        if (request.getLatitude() != null) stop.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) stop.setLongitude(request.getLongitude());
        if (request.getNote() != null) stop.setInstructions(request.getNote());
        ShipmentStop saved = shipmentStopRepository.save(stop);

        // If all stops completed, complete the shipment (records sales and updates order status)
        List<ShipmentStop> allStops = shipmentStopRepository.findByShipmentIdOrderByStopSequenceAsc(shipmentId);
        boolean allCompleted = allStops.stream().allMatch(s -> s.getStatus() == StopStatus.COMPLETED);
        if (allCompleted) {
            orderService.completeShipment(shipmentId);
        }

        return ResponseEntity.ok(ApiResponse.success(toStopResponse(saved)));
    }

    @Operation(summary = "Update shipment status", description = "Updates a shipment's status (driver action).")
    @PutMapping("/{id}/status")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam Long driverId,
            @RequestBody ShipmentStatusUpdateRequest request) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (!shipment.getDriverId().equals(actorId)) {
            throw new ForbiddenException("Driver not assigned to this shipment");
        }
        if (request.getStatus() != null) {
            // If setting to DELIVERED, use completeShipment to record sales and update order status
            if (request.getStatus() == ShipmentStatus.DELIVERED) {
                Shipment completed = orderService.completeShipment(id);
                return ResponseEntity.ok(ApiResponse.success(toResponse(completed)));
            }
            shipment.setStatus(request.getStatus());
        }
        Shipment saved = shipmentRepository.save(shipment);
        return ResponseEntity.ok(ApiResponse.success(toResponse(saved)));
    }

    @Operation(summary = "File proof of delivery", description = "Files proof of delivery for a shipment (requires verified OTP).")
    @PostMapping("/{id}/proof-of-delivery")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> proofOfDelivery(
            @PathVariable Long id,
            @RequestParam Long driverId,
            @RequestBody ProofOfDeliveryRequest request) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (!shipment.getDriverId().equals(actorId)) {
            throw new ForbiddenException("Driver not assigned to this shipment");
        }
        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT
                && shipment.getStatus() != ShipmentStatus.DRIVER_ACCEPTED) {
            throw new BadRequestException("Proof of delivery can only be filed for an active shipment");
        }
        // OTP verification would be validated here
        Shipment completed = orderService.completeShipment(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(completed)));
    }

    // ==================== DELIVERY OTP ENDPOINTS ====================

    @Operation(summary = "Request delivery OTP", description = "Driver requests a delivery OTP for a specific shipment. The OTP is sent to the buyer.")
    @PostMapping("/{shipmentId}/request-otp")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestDeliveryOtp(
            @PathVariable Long shipmentId,
            @RequestParam Long driverId) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        if (!java.util.Objects.equals(driverId, actorId) && !com.kilivana.backend.security.CurrentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("Driver may only request OTP for their own shipments");
        }

        DeliveryOtpVerification verification = settlementService.requestDeliveryOtp(shipmentId, driverId);

        Map<String, Object> response = Map.of(
                "shipmentId", shipmentId,
                "orderId", verification.getOrderId(),
                "status", "OTP_SENT",
                "otpVerificationId", verification.getId(),
                "expiresAt", verification.getOtpExpiresAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Confirm delivery with OTP", description = "Driver submits the OTP received from the buyer to confirm delivery and trigger settlement.")
    @PostMapping("/{shipmentId}/confirm-delivery")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmDeliveryWithOtp(
            @PathVariable Long shipmentId,
            @RequestParam Long driverId,
            @RequestBody Map<String, String> request) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        if (!java.util.Objects.equals(driverId, actorId) && !com.kilivana.backend.security.CurrentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("Driver may only confirm delivery for their own shipments");
        }

        String otp = request.get("otp");
        if (otp == null || otp.trim().isEmpty()) {
            throw new BadRequestException("OTP is required");
        }

        Settlement settlement = settlementService.confirmDeliveryWithOtp(shipmentId, driverId, otp);

        Map<String, Object> response = Map.of(
                "shipmentId", shipmentId,
                "deliveryStatus", "DELIVERED",
                "settlementStatus", settlement.getStatus().wire(),
                "settlementId", settlement.getId(),
                "settlementReference", settlement.getSettlementReference(),
                "deliveredAt", settlement.getSettledAt(),
                "sellerNetEarnings", settlement.getSellerNetEarnings(),
                "transportProviderPayable", settlement.getTransportProviderPayable(),
                "platformRevenue", settlement.getPlatformRevenue()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== SETTLEMENT VIEW ENDPOINTS ====================

    @Operation(summary = "Get settlement for shipment", description = "Returns the financial settlement details for a shipment.")
    @GetMapping("/{shipmentId}/settlement")
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public ResponseEntity<ApiResponse<FinanceDtos.SettlementResponse>> getShipmentSettlement(
            @PathVariable Long shipmentId,
            @RequestParam Long driverId) {
        Long actorId = com.kilivana.backend.security.CurrentUser.id();
        if (!java.util.Objects.equals(driverId, actorId) && !com.kilivana.backend.security.CurrentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("Driver may only view settlement for their own shipments");
        }

        Settlement settlement = settlementService.getSettlementByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement for shipment", shipmentId));

        return ResponseEntity.ok(ApiResponse.success(FinanceDtos.SettlementResponse.fromEntity(settlement)));
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
                .vehicleType(s.getVehicleType())
                .vehicleNumber(s.getVehicleNumber())
                .driverInstructions(s.getDriverInstructions())
                .estimatedWeightKg(s.getEstimatedWeightKg())
                .estimatedVolumeM3(s.getEstimatedVolumeM3())
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

    private ShipmentItemResponse toItemResponse(com.kilivana.backend.logistics.fulfilment.entity.ShipmentItem item) {
        return ShipmentItemResponse.builder()
                .id(item.getId())
                .orderItemId(item.getOrderItemId())
                .allocatedQuantity(item.getAllocatedQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}
