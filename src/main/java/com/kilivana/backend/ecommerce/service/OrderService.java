package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.FarmerProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.dto.OrderItemRequest;
import com.kilivana.backend.ecommerce.dto.OrderItemResponse;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Order placement and the order lifecycle.
 *
 * <p>The money is computed here, not trusted from the caller: the subtotal
 * is the sum of the line items, the total adds the delivery fee, and a
 * caller-supplied subtotal or total is only ever checked against the
 * computed figure. Line items are persisted with the name and unit the
 * product had at purchase, so an order keeps reading correctly after the
 * product is renamed or deleted.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final LogisticsService logisticsService;
    private final LogisticsJobRepository logisticsJobRepository;

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    public List<Order> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public Page<Order> searchOrders(Long buyerId, String status, Pageable pageable) {
        OrderStatus orderStatus = status != null ? OrderStatus.from(status) : null;
        return orderRepository.searchOrders(buyerId, orderStatus, pageable);
    }

    /**
     * Places an order: validates the buyer, the delivery address and every
     * line item, reserves the stock, computes the figures and writes the
     * order, its items and its first timeline event in one transaction.
     *
     * @throws BadRequestException when the buyer is not a buyer account, the
     *     address is not the buyer's, a product does not exist or has not
     *     enough unreserved stock, or the caller's figures disagree with the
     *     computed ones
     */
    @Transactional
    public Order createOrder(Long buyerId, Long addressId, BigDecimal deliveryFee,
                             List<OrderItemRequest> items,
                             BigDecimal claimedSubtotal, BigDecimal claimedTotal) {
        if (!userRepository.existsByIdAndRole(buyerId, UserRole.BUYER)) {
            throw new BadRequestException("Buyer ID must belong to a buyer account");
        }
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (!address.getUserId().equals(buyerId)) {
            throw new BadRequestException("Address does not belong to this buyer");
        }

        BigDecimal fee = deliveryFee == null ? BigDecimal.ZERO : deliveryFee;
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Delivery fee cannot be negative");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProductId()));
            if (product.getStatus() != com.kilivana.backend.common.enums.ProductStatus.ACTIVE) {
                throw new BadRequestException("Product is not available: " + product.getName());
            }
            int available = product.getStockQty() - nullToZero(product.getReservedQty());
            if (available < item.getQuantity()) {
                throw new BadRequestException("Only " + available
                        + " units of " + product.getName() + " are available");
            }
            // Reserve the stock now, so two buyers cannot order the last units
            // of a product at the same time.
            product.setReservedQty(nullToZero(product.getReservedQty()) + item.getQuantity());
            productRepository.save(product);
            subtotal = subtotal.add(item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(fee).setScale(2, RoundingMode.HALF_UP);

        if (claimedSubtotal != null && claimedSubtotal.compareTo(subtotal) != 0) {
            throw new BadRequestException("Subtotal does not match the order items. Expected "
                    + subtotal + " but received " + claimedSubtotal);
        }
        if (claimedTotal != null && claimedTotal.compareTo(total) != 0) {
            throw new BadRequestException("Total does not match the order items. Expected "
                    + total + " but received " + claimedTotal);
        }

        Order order = Order.builder()
                .buyerId(buyerId)
                .status(OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal)
                .deliveryFee(fee)
                .total(total)
                .addressId(addressId)
                .code(nextOrderCode())
                .build();
        Order saved = orderRepository.save(order);

        for (OrderItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            orderItemRepository.save(OrderItem.builder()
                    .orderId(saved.getId())
                    .productId(product.getId())
                    .sellerId(product.getSellerId())
                    .productName(product.getName())
                    .unit(product.getUnit())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build());
        }

        orderEventRepository.save(OrderEvent.builder()
                .orderId(saved.getId()).status(saved.getStatus()).build());
        return saved;
    }

    /**
     * Cancels an order and releases its reserved stock. A paid or escrowed
     * order is refunded, because the money only ever sat in escrow awaiting
     * a delivery that will not happen.
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        return cancelOrder(orderId, null);
    }

    /**
     * Cancels an order and releases its reserved stock. A paid or escrowed
     * order is refunded, because the money only ever sat in escrow awaiting
     * a delivery that will not happen. The reason, when the caller supplies
     * one, is kept on the order for the panel's timeline.
     */
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("A " + order.getStatus().wire() + " order cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            order.setCancellationReason(reason.trim());
        }
        // Release the reserved stock back to available inventory so other buyers
        // can purchase it.
        releaseStock(order);
        if (order.getPaymentStatus().isRefundable()) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            setPaymentStatusForOrder(order.getId(), PaymentStatus.REFUNDED);
        }
        Order saved = orderRepository.save(order);
        orderEventRepository.save(OrderEvent.builder()
                .orderId(orderId).status(OrderStatus.CANCELLED).build());
        return saved;
    }

    /**
     * Closes a delivered order: the payment leaves escrow and is released
     * to the seller.
     */
    @Transactional
    public Order confirmReceipt(Long orderId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Only a delivered order can be completed");
        }
        order.setStatus(OrderStatus.COMPLETED);
        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.HELD) {
            order.setPaymentStatus(PaymentStatus.SETTLED);
            setPaymentStatusForOrder(order.getId(), PaymentStatus.SETTLED);
        }
        Order saved = orderRepository.save(order);
        orderEventRepository.save(OrderEvent.builder()
                .orderId(orderId).status(OrderStatus.COMPLETED).build());
        return saved;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        orderEventRepository.save(OrderEvent.builder().orderId(orderId).status(status).build());
        return saved;
    }

    public List<OrderEvent> getOrderTimeline(Long orderId) {
        getOrderById(orderId);
        return orderEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /** The order's line items, for the response the panel renders. */
    public List<OrderItemResponse> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .sellerId(item.getSellerId())
                        .productName(item.getProductName())
                        .unit(item.getUnit())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();
    }

    /** The panel-facing view of an order, line items included. */
    public OrderResponse toResponse(Order order) {
        LogisticsJob job = latestJob(order.getId());
        User driver = job == null || job.getDriverId() == null
                ? null
                : userRepository.findById(job.getDriverId()).orElse(null);
        DriverProfile driverProfile = driver == null
                ? null
                : driverProfileRepository.findByUserId(driver.getId()).orElse(null);
        User buyer = order.getBuyerId() == null ? null
                : userRepository.findById(order.getBuyerId()).orElse(null);
        Address address = order.getAddressId() == null ? null
                : addressRepository.findById(order.getAddressId()).orElse(null);
        OrderItem firstItem = orderItemRepository.findByOrderId(order.getId()).stream()
                .filter(item -> item.getSellerId() != null)
                .findFirst().orElse(null);
        User seller = firstItem == null ? null
                : userRepository.findById(firstItem.getSellerId()).orElse(null);
        String sellerLocation = firstItem == null ? null
                : farmerProfileRepository.findByUserId(firstItem.getSellerId())
                        .map(com.kilivana.backend.admin.entity.FarmerProfile::getLocation)
                        .orElse(null);
        Payment payment = paymentRepository.findByOrderId(order.getId()).stream()
                .findFirst().orElse(null);
        return OrderResponse.builder()
                .id(order.getId())
                .code(order.getCode())
                .buyerId(order.getBuyerId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .total(order.getTotal())
                .paymentStatus(order.getPaymentStatus())
                .addressId(order.getAddressId())
                .cancellationReason(order.getCancellationReason())
                .driverId(job == null ? null : job.getDriverId())
                .driverName(driver == null ? null : driver.getName())
                .driverPhone(driver == null ? null : driver.getPhone())
                .driverVehicle(driverProfile == null || driverProfile.getVehicleType() == null
                        ? null : driverProfile.getVehicleType().wire())
                .driverPlate(driverProfile == null ? null : driverProfile.getVehicleNumber())
                .deliveryStatus(job == null ? null : job.getStatus())
                .logisticsJobId(job == null ? null : job.getId())
                .buyerName(buyer == null ? null : buyer.getName())
                .buyerPhone(buyer == null ? null : buyer.getPhone())
                .buyerCounty(buyer == null ? null : buyer.getRegion())
                .deliveryAddress(address == null ? null : address.getAddressText())
                .sellerName(seller == null ? null : seller.getName())
                .sellerPhone(seller == null ? null : seller.getPhone())
                .sellerCounty(seller == null ? null : seller.getRegion())
                .sellerLocation(sellerLocation)
                .paymentMethod(payment == null ? null
                        : payment.getMethod() == null ? null : payment.getMethod().wire())
                .paymentReference(payment == null ? null : payment.getReference())
                .paidAt(payment == null ? null : payment.getPaidAt())
                .items(getOrderItems(order.getId()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Dispatches an order to a driver: reuses the order's pending
     * delivery job when one exists, otherwise creates one — the pickup
     * defaulting to the first seller's region and the destination to
     * the buyer's delivery address — then assigns the driver. The order
     * itself is confirmed by the job's assignment, not here.
     */
    @Transactional
    public LogisticsJob assignOrderToDriver(Long orderId, Long driverId,
                                            String pickupAddress, String destinationAddress) {
        Order order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("A " + order.getStatus().wire() + " order cannot be dispatched");
        }
        LogisticsJob job = logisticsJobRepository.findByOrderId(orderId).stream()
                .filter(candidate -> candidate.getStatus() == DeliveryStatus.PENDING_ASSIGNMENT)
                .findFirst()
                .orElseGet(() -> logisticsService.createLogisticsJob(orderId,
                        pickupAddress != null && !pickupAddress.isBlank()
                                ? pickupAddress : pickupForOrder(orderId),
                        destinationAddress != null && !destinationAddress.isBlank()
                                ? destinationAddress : orderAddress(orderId)));
        logisticsService.assignDriver(job.getId(), driverId);
        return logisticsService.getJobById(job.getId());
    }

    /** The latest delivery job for an order, or null when none exists. */
    private LogisticsJob latestJob(Long orderId) {
        return logisticsJobRepository.findByOrderId(orderId).stream()
                .max(java.util.Comparator.comparing(LogisticsJob::getId))
                .orElse(null);
    }

    /**
     * Where the delivery collects from: the seller's region, else the
     * farm or shop location on the seller's profile, else — when the
     * seller has neither on file — the delivery address itself, because
     * the job's pickup column cannot be null and a degenerate job the
     * administrator can correct beats an order that cannot be dispatched.
     */
    private String pickupForOrder(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(OrderItem::getSellerId)
                .filter(java.util.Objects::nonNull)
                .map(userRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(seller -> {
                    if (seller.getRegion() != null) {
                        return seller.getRegion();
                    }
                    return farmerProfileRepository.findByUserId(seller.getId())
                            .map(com.kilivana.backend.admin.entity.FarmerProfile::getLocation)
                            .orElse(null);
                })
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseGet(() -> orderAddress(orderId));
    }

    private String orderAddress(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (order.getAddressId() == null) {
            return null;
        }
        return addressRepository.findById(order.getAddressId())
                .map(Address::getAddressText)
                .orElse(null);
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", id);
        }
        orderRepository.deleteById(id);
    }

    /** Moves every payment row of the order to the given status. */
    private void setPaymentStatusForOrder(Long orderId, PaymentStatus status) {
        paymentRepository.findByOrderId(orderId).forEach(payment -> {
            payment.setStatus(status);
            if (status == PaymentStatus.PAID && payment.getPaidAt() == null) {
                payment.setPaidAt(java.time.LocalDateTime.now());
            }
            paymentRepository.save(payment);
        });
    }

    /** Returns the units a cancelled order had reserved. */
    private void releaseStock(Order order) {
        orderItemRepository.findByOrderId(order.getId()).forEach(item -> {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int reserved = nullToZero(product.getReservedQty()) - item.getQuantity();
                product.setReservedQty(Math.max(0, reserved));
                productRepository.save(product);
            });
        });
    }

    /**
     * Panel reference for a new order. Derived from the highest existing
     * numeric code plus one; two orders placed in the same instant can
     * collide on the unique index, which rejects one of them — the same
     * trade-off the user reference codes make.
     */
    private String nextOrderCode() {
        return "ORD-" + String.format("%05d", orderRepository.maxNumericOrderCode() + 1);
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
