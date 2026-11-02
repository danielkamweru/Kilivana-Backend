package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, Long> {
    List<DeliveryEvent> findByDeliveryJobIdOrderByCreatedAtAsc(Long deliveryJobId);
}
