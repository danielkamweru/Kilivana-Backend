package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.UserPickupStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Access to the {@link UserPickupStation} table.
 * User pickup stations are saved stations for quick access by buyers.
 * The owner is carried on the row ({@code userId}) rather than implied by a relationship.
 */
@Repository
public interface UserPickupStationRepository extends JpaRepository<UserPickupStation, Long> {

    /**
     * Find all saved pickup stations for a user.
     */
    List<UserPickupStation> findByUserId(Long userId);

    /**
     * Find a specific saved pickup station for a user.
     */
    Optional<UserPickupStation> findByUserIdAndPickupStationId(Long userId, Long pickupStationId);

    /**
     * Find the default pickup station for a user.
     */
    Optional<UserPickupStation> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Delete all saved pickup stations for a user.
     */
    void deleteByUserId(Long userId);
}
