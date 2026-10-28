package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.PickupStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to the {@link PickupStation} table.
 * Pickup stations are platform-managed locations where buyers can collect orders.
 */
@Repository
public interface PickupStationRepository extends JpaRepository<PickupStation, Long> {

    /**
     * Find all available pickup stations.
     */
    List<PickupStation> findByIsAvailableTrue();

    /**
     * Find pickup stations by region.
     */
    List<PickupStation> findByRegion(String region);

    /**
     * Find available pickup stations by region.
     */
    List<PickupStation> findByRegionAndIsAvailableTrue(String region);
}
