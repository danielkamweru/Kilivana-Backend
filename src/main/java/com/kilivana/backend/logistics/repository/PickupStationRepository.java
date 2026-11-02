package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.PickupStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Prefilter for nearest-station search: every available station whose bounding box
     * overlaps the search circle. The final radius filter and nearest-first sort are
     * done in {@code PickupStationService} with a Haversine calculation, because the
     * bounding box alone is not a distance and the in-memory sort is trivial to read
     * and unit-test.
     *
     * <p>The box is generous (1 deg lat ~= 111 km, 1 deg lng ~= 111 * cos(lat) km),
     * so it can only ever return a superset of the true answer; nothing real is dropped.
     */
    @Query(value =
            "SELECT ps FROM PickupStation ps " +
            "WHERE ps.isAvailable = true " +
            "  AND ps.latitude IS NOT NULL AND ps.longitude IS NOT NULL " +
            "  AND ps.latitude BETWEEN :latMin AND :latMax " +
            "  AND ps.longitude BETWEEN :lngMin AND :lngMax")
    List<PickupStation> findNearestCandidates(
            @Param("latMin") double latMin,
            @Param("latMax") double latMax,
            @Param("lngMin") double lngMin,
            @Param("lngMax") double lngMax);
}