package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    public List<Order> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public Page<Order> searchOrders(Long buyerId, String status, Pageable pageable) {
        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status) : null;
        return orderRepository.searchOrders(buyerId, orderStatus, pageable);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        orderEventRepository.save(OrderEvent.builder().orderId(orderId).status(status).build());
        return saved;
    }

    @Transactional
    public Order createOrder(Long buyerId, BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal total, Long addressId) {
        Order order = Order.builder()
                .buyerId(buyerId)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .addressId(addressId)
                .build();
        Order saved = orderRepository.save(order);
        orderEventRepository.save(OrderEvent.builder().orderId(saved.getId()).status(saved.getStatus()).build());
        return saved;
    }

    public List<OrderEvent> getOrderTimeline(Long orderId) {
        getOrderById(orderId);
        return orderEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", id);
        }
        orderRepository.deleteById(id);
    }
}
