package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The status history for delivery jobs. Lower frequency than the position stream, but
 * carries the status transitions with the position each was recorded at.
 */
@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    /** The events for a job, most recent first. */
    List<TrackingEvent> findByLogisticsJobIdOrderByRecordedAtDesc(Long logisticsJobId);

    /** Every event recorded by one driver. */
    List<TrackingEvent> findByDriverId(Long driverId);
}