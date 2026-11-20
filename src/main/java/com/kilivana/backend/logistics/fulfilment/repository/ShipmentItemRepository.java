package com.kilivana.backend.logistics.fulfilment.repository;

import com.kilivana.backend.logistics.fulfilment.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for shipment items.
 */
@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {

    List<ShipmentItem> findByShipmentId(Long shipmentId);

    List<ShipmentItem> findByOrderItemId(Long orderItemId);

    @Query("SELECT si FROM ShipmentItem si WHERE si.shipmentId = :shipmentId AND si.orderItemId = :orderItemId")
    Optional<ShipmentItem> findByShipmentIdAndOrderItemId(@Param("shipmentId") Long shipmentId,
                                                           @Param("orderItemId") Long orderItemId);
}
