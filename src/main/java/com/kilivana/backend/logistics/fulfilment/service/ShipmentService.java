package com.kilivana.backend.logistics.fulfilment.service;

import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-facing shipment management: creating shipments from paid orders,
 * assigning drivers and vehicles, creating route stops, cancelling and
 * reassigning shipments.
 * <p>
 * Shipment-level quantity allocation is guarded so that the same quantity of
 * an order item is never allocated to two active shipments, and allocated
 * quantities never exceed the quantity purchased.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final ShipmentStopRepository shipmentStopRepository;
    private final SellerFulfilmentRepository fulfilmentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;

    /**
     * Creates a new shipment for a paid order. The admin selects which seller
     * fulfilments (and therefore which order items) belong to this shipment.
     *
     * @param orderId the paid customer order
     * @param sellerFulfilmentIds the fulfilment records to include in this shipment
     * @param driverId the driver to assign (optional)
     * @param vehicleType the vehicle type (optional)
     * @param vehicleNumber the vehicle plate (optional)
     * @param driverInstructions instructions for the driver
     * @return the created shipment
     */
    @Transactional
    public Shipment createShipment(Long orderId,
                                   List<Long> sellerFulfilmentIds,
                                   Long driverId,
                                   String vehicleType,
                                   String vehicleNumber,
                                   String driverInstructions) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!isPaid(order)) {
            throw new BadRequestException("Only a paid order can be shipped");
        }

        if (sellerFulfilmentIds == null || sellerFulfilmentIds.isEmpty()) {
            throw new BadRequestException("A shipment must include at least one seller fulfilment");
        }

        // Collect all order items from the selected fulfilments.
        Map<Long, OrderItem> itemsToAllocate = new LinkedHashMap<>();
        List<OrderItem> allOrderItems = orderItemRepository.findByOrderId(orderId);
        for (Long fulfilmentId : sellerFulfilmentIds) {
            SellerFulfilment fulfilment = fulfilmentRepository.findById(fulfilmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("SellerFulfilment", fulfilmentId));
            if (!fulfilment.getOrderId().equals(orderId)) {
                throw new BadRequestException("Fulfilment " + fulfilmentId + " does not belong to order " + orderId);
            }
            for (OrderItem item : allOrderItems) {
                if (item.getSellerId().equals(fulfilment.getSellerId())) {
                    itemsToAllocate.put(item.getId(), item);
                }
            }
        }

        if (itemsToAllocate.isEmpty()) {
            throw new BadRequestException("No order items found for the selected fulfilments");
        }

        // Validate that no quantity is already allocated to another non-terminal shipment.
        for (OrderItem item : itemsToAllocate.values()) {
            int alreadyAllocated = alreadyAllocated(item.getId());
            if (alreadyAllocated >= item.getQuantity()) {
                throw new BadRequestException(
                        "Order item " + item.getId() + " (" + item.getProductName() +
                        ") is already fully allocated to other shipments");
            }
        }

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .code(nextShipmentCode())
                .status(ShipmentStatus.PENDING)
                .driverId(driverId)
                .vehicleType(vehicleType)
                .vehicleNumber(vehicleNumber == null ? null : vehicleNumber.toUpperCase())
                .driverInstructions(driverInstructions)
                .build();
        Shipment saved = shipmentRepository.save(shipment);

        // Create shipment items, allocating remaining quantity from each order item.
        for (OrderItem item : itemsToAllocate.values()) {
            int alreadyAllocated = alreadyAllocated(item.getId());
            int remaining = item.getQuantity() - alreadyAllocated;
            if (remaining <= 0) {
                continue;
            }
            ShipmentItem shipmentItem = ShipmentItem.builder()
                    .shipmentId(saved.getId())
                    .orderItemId(item.getId())
                    .allocatedQuantity(remaining)
                    .unitPrice(item.getUnitPrice())
                    .build();
            shipmentItemRepository.save(shipmentItem);
        }

        log.info("Created shipment {} for order {} with {} items", saved.getId(), orderId, itemsToAllocate.size());
        return saved;
    }

    /**
     * Creates route stops for a shipment: one PICKUP stop per distinct seller
     * included in the shipment's items, then one DELIVERY stop for the buyer.
     */
    @Transactional
    public Shipment createStops(Long shipmentId,
                                List<Long> pickupSellerIds,
                                String deliveryAddress,
                                Double deliveryLatitude,
                                Double deliveryLongitude) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        Order order = orderRepository.findById(shipment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", shipment.getOrderId()));

        List<OrderItem> allOrderItems = orderItemRepository.findByOrderId(shipment.getOrderId());

        int sequence = 1;
        List<ShipmentStop> stops = new ArrayList<>();

        // One pickup stop per distinct seller included in this shipment.
        Map<Long, String> sellerAddresses = new LinkedHashMap<>();
        for (Long sellerId : pickupSellerIds) {
            List<OrderItem> sellerItems = allOrderItems.stream()
                    .filter(item -> sellerId.equals(item.getSellerId()))
                    .toList();
            if (sellerItems.isEmpty()) {
                throw new BadRequestException("No items for seller " + sellerId + " in order " + shipment.getOrderId());
            }
            OrderItem first = sellerItems.get(0);
            String address = first.getDispatchAddressSnapshot();
            if (address == null || address.isBlank()) {
                address = first.getDispatchLocationNameSnapshot();
            }
            if (address == null || address.isBlank()) {
                address = "Seller " + sellerId;
            }
            sellerAddresses.put(sellerId, address);
        }

        for (Map.Entry<Long, String> entry : sellerAddresses.entrySet()) {
            ShipmentStop stop = ShipmentStop.builder()
                    .shipmentId(shipmentId)
                    .stopType(StopType.PICKUP)
                    .partyId(entry.getKey())
                    .address(entry.getValue())
                    .latitude(null)
                    .longitude(null)
                    .stopSequence(sequence++)
                    .status(StopStatus.PENDING)
                    .instructions("Collect products from seller " + entry.getKey())
                    .build();
            stops.add(shipmentStopRepository.save(stop));
        }

        // Delivery stop last.
        ShipmentStop deliveryStop = ShipmentStop.builder()
                .shipmentId(shipmentId)
                .stopType(StopType.DELIVERY)
                .partyId(order.getBuyerId())
                .address(deliveryAddress != null && !deliveryAddress.isBlank()
                        ? deliveryAddress : resolveOrderAddress(order))
                .latitude(deliveryLatitude)
                .longitude(deliveryLongitude)
                .stopSequence(sequence)
                .status(StopStatus.PENDING)
                .instructions("Deliver to buyer " + order.getBuyerId())
                .build();
        stops.add(shipmentStopRepository.save(deliveryStop));

        log.info("Created {} stops for shipment {}", stops.size(), shipmentId);
        return shipment;
    }

    /**
     * Assigns a driver and vehicle to a pending shipment.
     */
    @Transactional
    public Shipment assignDriver(Long shipmentId, Long driverId) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        if (shipment.getStatus() != ShipmentStatus.PENDING) {
            throw new BadRequestException("A shipment in status " + shipment.getStatus().wire()
                    + " cannot be assigned");
        }

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        if (driver.getRole() != UserRole.DRIVER) {
            throw new BadRequestException("User " + driverId + " is not a driver");
        }
        if (driver.getStatus() != UserStatus.ACTIVE
                || (driver.getVerificationStatus() != VerificationStatus.VERIFIED
                && driver.getVerificationStatus() != VerificationStatus.NOT_REQUIRED)) {
            throw new BadRequestException("Driver account is not active and verified");
        }

        DriverProfile profile = driverProfileRepository.findByUserId(driverId)
                .orElseThrow(() -> new BadRequestException("Driver has no verified driver profile"));
        if (profile.getKycStatus() != KycStatus.VERIFIED
                || profile.getAvailabilityStatus() != DriverStatus.AVAILABLE) {
            throw new BadRequestException("Driver is not active, verified and available");
        }

        // Check driver is not already assigned to another non-terminal shipment.
        List<Shipment> activeShipments = shipmentRepository.findByDriverId(driverId);
        boolean hasActive = activeShipments.stream()
                .anyMatch(s -> !s.getId().equals(shipmentId) && s.getStatus().isOpen());
        if (hasActive) {
            throw new BadRequestException("Driver is already assigned to an active shipment");
        }

        shipment.setDriverId(driverId);
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        shipment.setAssignedAt(LocalDateTime.now());
        Shipment saved = shipmentRepository.save(shipment);

        profile.setAvailabilityStatus(DriverStatus.ON_DELIVERY);
        driverProfileRepository.save(profile);

        log.info("Assigned driver {} to shipment {}", driverId, shipmentId);
        return saved;
    }

    /**
     * Reassigns a shipment to a different driver, releasing the previous one.
     */
    @Transactional
    public Shipment reassign(Long shipmentId, Long newDriverId) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        if (!shipment.getStatus().isOpen()) {
            throw new BadRequestException("A " + shipment.getStatus().wire() + " shipment cannot be reassigned");
        }

        // Release previous driver.
        if (shipment.getDriverId() != null) {
            DriverProfile profile = driverProfileRepository.findByUserId(shipment.getDriverId()).orElse(null);
            if (profile != null && profile.getAvailabilityStatus() == DriverStatus.ON_DELIVERY) {
                profile.setAvailabilityStatus(DriverStatus.AVAILABLE);
                driverProfileRepository.save(profile);
            }
        }

        return assignDriver(shipmentId, newDriverId);
    }

    /**
     * Cancels a shipment. Does NOT cancel or refund the original paid order.
     * Allocations are released so the items can be re-shipped.
     */
    @Transactional
    public Shipment cancel(Long shipmentId, String reason) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new BadRequestException("A delivered shipment cannot be cancelled");
        }
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            return shipment;
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancellationReason(reason);
        shipment.setCancelledAt(LocalDateTime.now());

        // Release the driver.
        if (shipment.getDriverId() != null) {
            DriverProfile profile = driverProfileRepository.findByUserId(shipment.getDriverId()).orElse(null);
            if (profile != null && profile.getAvailabilityStatus() == DriverStatus.ON_DELIVERY) {
                profile.setAvailabilityStatus(DriverStatus.AVAILABLE);
                driverProfileRepository.save(profile);
            }
        }

        // Release all shipment item allocations (they can be re-allocated to another shipment).
        List<ShipmentItem> items = shipmentItemRepository.findByShipmentId(shipmentId);
        for (ShipmentItem item : items) {
            shipmentItemRepository.delete(item);
        }

        // Cancel all stops.
        List<ShipmentStop> stops = shipmentStopRepository.findByShipmentIdOrderByStopSequenceAsc(shipmentId);
        for (ShipmentStop stop : stops) {
            stop.setStatus(StopStatus.FAILED);
            shipmentStopRepository.save(stop);
        }

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Cancelled shipment {}: {}", shipmentId, reason);
        return saved;
    }

    @Transactional(readOnly = true)
    public Shipment getShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
    }

    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsForOrder(Long orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsForDriver(Long driverId) {
        return shipmentRepository.findByDriverId(driverId);
    }

    private boolean isPaid(Order order) {
        return order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.HELD
                || order.getPaymentStatus() == PaymentStatus.SETTLED;
    }

    private String resolveOrderAddress(Order order) {
        if (order.getPickupLocation() != null && !order.getPickupLocation().isBlank()) {
            return order.getPickupLocation();
        }
        if (order.getPickupStationAddressSnapshot() != null) {
            return order.getPickupStationAddressSnapshot();
        }
        return null;
    }

    private int alreadyAllocated(Long orderItemId) {
        return shipmentItemRepository.findByOrderItemId(orderItemId).stream()
                .mapToInt(si -> si.getAllocatedQuantity() == null ? 0 : si.getAllocatedQuantity())
                .sum();
    }

    private String nextShipmentCode() {
        return "SHIP-" + String.format("%05d", shipmentRepository.count() + 1);
    }
}
