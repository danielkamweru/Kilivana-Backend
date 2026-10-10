package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Access to the {@link DriverProfile} table. A driver profile is optional — a registered driver
 * may never have onboarded one — so reads return {@link Optional}.
 */
@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    Optional<DriverProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}