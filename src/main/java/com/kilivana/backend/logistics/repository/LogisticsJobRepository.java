package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.common.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogisticsJobRepository extends JpaRepository<LogisticsJob, Long> {
    
    List<LogisticsJob> findByOrderId(Long orderId);
    
    List<LogisticsJob> findByDriverId(Long driverId);
    
    List<LogisticsJob> findByStatus(DeliveryStatus status);
    
    List<LogisticsJob> findByDriverIdAndStatus(Long driverId, DeliveryStatus status);

    /**
     * Completed deliveries per driver, in one round trip. The admin driver list shows a
     * delivery count for every driver, so counting per driver in a loop would issue a query
     * per row of the list.
     */
    @Query("SELECT j.driverId, count(j) FROM LogisticsJob j "
            + "WHERE j.driverId IS NOT NULL AND j.status = com.kilivana.backend.common.enums.DeliveryStatus.DELIVERED "
            + "GROUP BY j.driverId")
    List<Object[]> countDeliveredByDriver();

    /** The order a driver is currently running, if any. At most one job is in flight. */
    @Query("SELECT j FROM LogisticsJob j WHERE j.driverId = :driverId "
            + "AND j.status <> com.kilivana.backend.common.enums.DeliveryStatus.DELIVERED "
            + "AND j.status <> com.kilivana.backend.common.enums.DeliveryStatus.CANCELLED "
            + "AND j.status <> com.kilivana.backend.common.enums.DeliveryStatus.FAILED")
    List<LogisticsJob> findActiveByDriverId(@Param("driverId") Long driverId);
}
