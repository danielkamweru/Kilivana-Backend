package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.DriverLatestLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * One row per delivery job: the driver's most recent fix, so a "where is the driver now"
 * read is a single row lookup instead of a history scan. A denormalised cache of the
 * latest driver_locations row, kept in sync inside the same transaction.
 */
public interface DriverLatestLocationRepository extends JpaRepository<DriverLatestLocation, Long> {

    /** The cached latest fix for a delivery job, or empty when none has been recorded yet. */
    Optional<DriverLatestLocation> findByLogisticsJobId(Long logisticsJobId);
}