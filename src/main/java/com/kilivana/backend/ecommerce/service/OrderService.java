package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.admin.repository.UserRepository;
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
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
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
                .build();
        Order saved = orderRepository.save(order);
        saved.setCode(orderCode(saved.getId()));
        orderRepository.save(saved);

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
        Order order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("A " + order.getStatus().wire() + " order cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
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
                .items(getOrderItems(order.getId()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
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

    private static String orderCode(Long id) {
        return "ORD-" + String.format("%05d", id);
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
