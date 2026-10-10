package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The high-frequency position stream for active deliveries. Append-only: a fix is never
 * edited or deleted by the business layer. Retention is handled by the prune query below,
 * which removes fixes older than the configured retention window; the delivery record
 * itself (the job, the proof, the latest fix) is never deleted.
 */
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {

    /** Every fix for a job, oldest first, for replaying a journey. */
    List<DriverLocation> findByLogisticsJobIdOrderByRecordedAtAsc(Long logisticsJobId);

    /** Every fix recorded by one driver, most recent first. */
    List<DriverLocation> findByDriverIdOrderByRecordedAtDesc(Long driverId);

    /** Prunes fixes older than the cutoff. The delivery record itself is never touched. */
    @Modifying
    @Transactional
    @Query("DELETE FROM DriverLocation d WHERE d.recordedAt < :cutoff")
    int deleteByRecordedAtBefore(@Param("cutoff") Instant cutoff);
}