package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.enums.DeliveryTripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryTripRepository extends JpaRepository<DeliveryTrip, Long> {
    List<DeliveryTrip> findByDeliveryJobIdOrderByCreatedAtDesc(Long deliveryJobId);
    List<DeliveryTrip> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<DeliveryTrip> findByDriverIdAndStatusOrderByCreatedAtDesc(Long driverId, DeliveryTripStatus status);
    Optional<DeliveryTrip> findFirstByDeliveryJobIdAndDriverIdAndStatusOrderByCreatedAtDesc(
            Long deliveryJobId, Long driverId, DeliveryTripStatus status);
}
