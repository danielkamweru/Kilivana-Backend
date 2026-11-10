package com.kilivana.backend.logistics.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.entity.SupplierWarehouse;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.admin.repository.SupplierWarehouseRepository;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.logistics.dto.DispatchOrderResponse;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.PickupStation;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryDispatchService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final LogisticsJobRepository jobRepository;
    private final LogisticsService logisticsService;
    private final FarmRepository farmRepository;
    private final SupplierWarehouseRepository warehouseRepository;
    private final AddressRepository addressRepository;
    private final PickupStationRepository stationRepository;

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public DispatchOrderResponse dispatchOrder(Long orderId) {
        return dispatchOrder(orderId, false);
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public DispatchOrderResponse dispatchSingleSeller(Long orderId) {
        return dispatchOrder(orderId, true);
    }

    private DispatchOrderResponse dispatchOrder(Long orderId, boolean requireSingleSeller) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (order.getPaymentStatus() != PaymentStatus.PAID
                && order.getPaymentStatus() != PaymentStatus.HELD
                && order.getPaymentStatus() != PaymentStatus.SETTLED) {
            throw new BadRequestException("Only a paid order can be dispatched");
        }
        if (order.getStatus() == null || order.getStatus() == com.kilivana.backend.common.enums.OrderStatus.CANCELLED
                || order.getStatus() == com.kilivana.backend.common.enums.OrderStatus.DISPUTED
                || order.getStatus() == com.kilivana.backend.common.enums.OrderStatus.COMPLETED
                || order.getStatus() == com.kilivana.backend.common.enums.OrderStatus.DELIVERED) {
            throw new BadRequestException("This order is not eligible for dispatch");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new BadRequestException("An order without line items cannot be dispatched");
        }
        Map<DispatchKey, List<OrderItem>> groups = new LinkedHashMap<>();
        List<Map<String, Object>> invalidItems = new ArrayList<>();
        for (OrderItem item : items) {
            if (item.getSellerId() == null || item.getDispatchLocationIdSnapshot() == null
                    || item.getDispatchLocationTypeSnapshot() == null) {
                Map<String, Object> detail = new java.util.LinkedHashMap<>();
                detail.put("orderItemId", item.getId());
                detail.put("productId", item.getProductId());
                detail.put("productName", item.getProductName());
                detail.put("sellerId", item.getSellerId());
                detail.put("reason", "NO_ACTIVE_DISPATCH_LOCATION");
                invalidItems.add(detail);
                continue;
            }
            if ("DELETED_PRODUCT".equals(item.getDispatchLocationTypeSnapshot())) {
                Map<String, Object> detail = new java.util.LinkedHashMap<>();
                detail.put("orderItemId", item.getId());
                detail.put("productId", item.getProductId());
                detail.put("productName", item.getProductName());
                detail.put("sellerId", item.getSellerId());
                detail.put("reason", "PRODUCT_DELETED");
                invalidItems.add(detail);
                continue;
            }
            DispatchKey key = new DispatchKey(item.getSellerId(),
                    item.getDispatchLocationTypeSnapshot(), item.getDispatchLocationIdSnapshot());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        if (!invalidItems.isEmpty()) {
            throw new com.kilivana.backend.common.exception.DispatchLocationMissingException(
                    orderId, invalidItems);
        }
        if (requireSingleSeller && groups.size() != 1) {
            throw new BadRequestException("This order has multiple dispatch origins; use the dispatch endpoint");
        }

        Destination destination = resolveDestination(order);
        List<LogisticsJob> jobs = new ArrayList<>();
        for (Map.Entry<DispatchKey, List<OrderItem>> entry : groups.entrySet()) {
            DispatchKey key = entry.getKey();
            OrderItem first = entry.getValue().get(0);
            requireSameOrigin(entry.getValue(), first);
            LogisticsJob existing = jobRepository.findByOrderIdAndSellerId(orderId, key.sellerId()).stream()
                    .filter(job -> sameOrigin(job, key)
                            && job.getStatus() != com.kilivana.backend.common.enums.DeliveryStatus.CANCELLED
                            && job.getStatus() != com.kilivana.backend.common.enums.DeliveryStatus.FAILED
                            && job.getStatus() != com.kilivana.backend.common.enums.DeliveryStatus.DELIVERED)
                    .findFirst().orElse(null);
            if (existing != null) {
                jobs.add(existing);
                continue;
            }
            validateOrigin(first);
            validateLocationOwnership(key);
            DispatchOriginLocation origin = resolveOrigin(first, key);
            LogisticsJob request = LogisticsJob.builder()
                    .orderId(orderId)
                    .sellerId(key.sellerId())
                    .buyerId(order.getBuyerId())
                    .pickupFarmId("FARM".equals(key.type()) ? key.locationId() : null)
                    .pickupWarehouseId("WAREHOUSE".equals(key.type()) ? key.locationId() : null)
                    .pickupStationId(destination.stationId())
                    .destinationType(destination.type())
                    .pickupAddress(origin.address())
                    .pickupLatitude(origin.latitude())
                    .pickupLongitude(origin.longitude())
                    .destinationAddress(destination.address())
                    .destinationLatitude(destination.latitude())
                    .destinationLongitude(destination.longitude())
                    .cargoDescription(entry.getValue().stream().map(OrderItem::getProductName).distinct()
                            .collect(java.util.stream.Collectors.joining(", ")))
                    .quantity(entry.getValue().stream().mapToInt(OrderItem::getQuantity).sum())
                    .build();
            LogisticsJob created = logisticsService.createJob(request);
            jobs.add(created);
        }
        return DispatchOrderResponse.builder().orderId(orderId).deliveryJobs(List.copyOf(jobs)).build();
    }

    private Destination resolveDestination(Order order) {
        if ("DOOR_DELIVERY".equalsIgnoreCase(order.getDeliveryMethod())) {
            if (order.getAddressId() == null) {
                throw new BadRequestException("A delivery address is required for dispatch");
            }
            Address address = addressRepository.findById(order.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", order.getAddressId()));
            if (!order.getBuyerId().equals(address.getUserId())) {
                throw new BadRequestException("The delivery address does not belong to this buyer");
            }
            validateCoordinates(address.getLatitude(), address.getLongitude(), address.getAddressText());
            return new Destination(null, "HOME", address.getAddressText(),
                    address.getLatitude(), address.getLongitude());
        }
        if (order.getPickupStationId() != null) {
            validateCoordinates(order.getPickupStationLatitudeSnapshot(),
                    order.getPickupStationLongitudeSnapshot(), order.getPickupStationAddressSnapshot());
            return new Destination(order.getPickupStationId(), "PICKUP_STATION",
                    order.getPickupStationAddressSnapshot(), order.getPickupStationLatitudeSnapshot(),
                    order.getPickupStationLongitudeSnapshot());
        }
        if (order.getAddressId() != null && "PICKUP_STATION".equalsIgnoreCase(order.getDeliveryMethod())) {
            PickupStation station = stationRepository.findById(order.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("PickupStation", order.getAddressId()));
            validateCoordinates(station.getLatitude(), station.getLongitude(), station.getAddress());
            return new Destination(station.getId(), "PICKUP_STATION", station.getAddress(),
                    station.getLatitude(), station.getLongitude());
        }
        throw new BadRequestException(
                "A dispatchable pickup station is required; legacy manual locations need verified coordinates");
    }

    private static void validateOrigin(OrderItem item) {
        if (item.getDispatchLocationTypeSnapshot() == null
                || !List.of("FARM", "WAREHOUSE").contains(item.getDispatchLocationTypeSnapshot())) {
            throw new BadRequestException("Product has an unsupported dispatch location type");
        }
    }

    private DispatchOriginLocation resolveOrigin(OrderItem item, DispatchKey key) {
        if (hasValidCoordinates(item.getDispatchAddressSnapshot(), item.getDispatchLatitudeSnapshot(),
                item.getDispatchLongitudeSnapshot())) {
            return new DispatchOriginLocation(item.getDispatchAddressSnapshot(),
                    item.getDispatchLatitudeSnapshot(), item.getDispatchLongitudeSnapshot());
        }

        if ("FARM".equals(key.type())) {
            Farm farm = farmRepository.findById(key.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm", key.locationId()));
            validateCoordinates(farm.getLatitude(), farm.getLongitude(), farm.getAddress());
            return new DispatchOriginLocation(farm.getAddress(), farm.getLatitude(), farm.getLongitude());
        }

        SupplierWarehouse warehouse = warehouseRepository.findByIdAndUserId(key.locationId(), key.sellerId())
                .orElseThrow(() -> new BadRequestException(
                        "The product's warehouse does not belong to its supplier"));
        validateCoordinates(warehouse.getLatitude(), warehouse.getLongitude(), warehouse.getAddress());
        return new DispatchOriginLocation(warehouse.getAddress(), warehouse.getLatitude(), warehouse.getLongitude());
    }

    private static boolean hasValidCoordinates(String address, Double latitude, Double longitude) {
        return address != null && !address.isBlank()
                && latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    private void validateLocationOwnership(DispatchKey key) {
        if ("FARM".equals(key.type())) {
            Farm farm = farmRepository.findById(key.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm", key.locationId()));
            if (!key.sellerId().equals(farm.getFarmerId())
                    || farm.getStatus() != com.kilivana.backend.common.enums.FarmStatus.ACTIVE) {
                throw new BadRequestException("The product's farm is no longer an active seller location");
            }
            return;
        }
        if ("WAREHOUSE".equals(key.type())) {
            SupplierWarehouse warehouse = warehouseRepository.findByIdAndUserId(key.locationId(), key.sellerId())
                    .orElseThrow(() -> new BadRequestException(
                            "The product's warehouse does not belong to its supplier"));
            if (!Boolean.TRUE.equals(warehouse.getActive())) {
                throw new BadRequestException("The product's warehouse is no longer active");
            }
            return;
        }
        throw new BadRequestException("Product has an unsupported dispatch location type");
    }

    private static void validateCoordinates(Double latitude, Double longitude, String address) {
        if (address == null || address.isBlank() || latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("Dispatch requires an address and valid latitude/longitude coordinates");
        }
    }

    private static void requireSameOrigin(List<OrderItem> items, OrderItem first) {
        boolean mismatch = items.stream().anyMatch(item ->
                !java.util.Objects.equals(item.getDispatchLocationTypeSnapshot(),
                        first.getDispatchLocationTypeSnapshot())
                        || !java.util.Objects.equals(item.getDispatchLocationIdSnapshot(),
                        first.getDispatchLocationIdSnapshot()));
        if (mismatch) {
            throw new BadRequestException("A seller delivery job cannot combine different pickup locations");
        }
    }

    private static boolean sameOrigin(LogisticsJob job, DispatchKey key) {
        return "FARM".equals(key.type())
                ? java.util.Objects.equals(job.getPickupFarmId(), key.locationId())
                : "WAREHOUSE".equals(key.type())
                && java.util.Objects.equals(job.getPickupWarehouseId(), key.locationId());
    }

    private record DispatchKey(Long sellerId, String type, Long locationId) {}
    private record DispatchOriginLocation(String address, Double latitude, Double longitude) {}
    private record Destination(Long stationId, String type, String address, Double latitude, Double longitude) {}
}
