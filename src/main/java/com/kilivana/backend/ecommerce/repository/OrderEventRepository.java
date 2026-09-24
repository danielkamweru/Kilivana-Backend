package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}