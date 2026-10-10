package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.common.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Delivery jobs: the core entity of the logistics domain. Queries here are read-only
 * apart from the retention prune; the state machine and the order/payout logic live in
 * {@code LogisticsService}, so the repository is a thin persistence layer.
 */
@Repository
public interface LogisticsJobRepository extends JpaRepository<LogisticsJob, Long> {

    /** Every delivery job for one order. */
    List<LogisticsJob> findByOrderId(Long orderId);

    /** Every delivery job assigned to one driver. */
    List<LogisticsJob> findByDriverId(Long driverId);

    /** Every delivery job in one status. */
    List<LogisticsJob> findByStatus(DeliveryStatus status);

    /** The jobs of one driver that are currently in one status. */
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

    /**
     * The order a driver is currently running, if any. At most one job is in flight per
     * driver at a time, so this returns at most one row.
     */
    @Query("SELECT j FROM LogisticsJob j WHERE j.driverId = :driverId "
            + "AND j.status <> com.kilivana.backend.common.enums.DeliveryStatus.DELIVERED "
            + "AND j.status <> com.kilivana.backend.common.enums.DeliveryStatus.CANCELLED "
            + "AND j.status <> com.kilivana.backend.common.enums.DeliveryStatus.FAILED")
    List<LogisticsJob> findActiveByDriverId(@Param("driverId") Long driverId);
}