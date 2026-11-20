package com.kilivana.backend.logistics.fulfilment.repository;

import com.kilivana.backend.logistics.fulfilment.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for shipments.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findByOrderId(Long orderId);

    List<Shipment> findByDriverId(Long driverId);

    Optional<Shipment> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shipment s WHERE s.id = :id")
    Optional<Shipment> findByIdForUpdate(@Param("id") Long id);

    List<Shipment> findByStatus(com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus status);
}
