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

    Optional<DriverLatestLocation> findByLogisticsJobId(Long logisticsJobId);
}