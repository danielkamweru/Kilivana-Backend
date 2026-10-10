package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.FarmImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link FarmImage} entities.
 * <p>
 * Images for a farm are ordered by {@code sortOrder} ascending so clients
 * can display them in a deterministic sequence. The primary image lookup
 * returns at most one result per farm; the application layer is responsible
 * for enforcing the "one primary per farm" invariant.
 */
@Repository
public interface FarmImageRepository extends JpaRepository<FarmImage, Long> {
    List<FarmImage> findByFarmIdOrderBySortOrderAsc(Long farmId);

    java.util.Optional<FarmImage> findByFarmIdAndIsPrimaryTrue(Long farmId);
}