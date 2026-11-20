package com.kilivana.backend.logistics.fulfilment.repository;

import com.kilivana.backend.logistics.fulfilment.entity.ShipmentStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for shipment stops.
 */
@Repository
public interface ShipmentStopRepository extends JpaRepository<ShipmentStop, Long> {

    List<ShipmentStop> findByShipmentIdOrderByStopSequenceAsc(Long shipmentId);

    List<ShipmentStop> findByShipmentIdAndStopType(Long shipmentId, com.kilivana.backend.logistics.fulfilment.enums.StopType stopType);

    Optional<ShipmentStop> findByShipmentIdAndStopSequence(@Param("shipmentId") Long shipmentId,
                                                           @Param("stopSequence") Integer stopSequence);

    List<ShipmentStop> findByStatus(com.kilivana.backend.logistics.fulfilment.enums.StopStatus status);
}
