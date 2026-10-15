package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link Farm} entities.
 * <p>
 * Exposes both derived queries (by farmer, by county) and a flexible
 * {@link #searchFarms(String, String, com.kilivana.backend.common.enums.FarmStatus, Long, Pageable)}
 * query that supports optional free-text, county, status, and farmer filters.
 * Null parameters are treated as "no filter" so the same query can be reused
 * for both broad and narrow searches.
 */
@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {
    List<Farm> findByFarmerId(Long farmerId);

    Page<Farm> findByCountyIgnoreCase(String county, Pageable pageable);

    @Query("SELECT f FROM Farm f WHERE " +
           "(:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(f.county) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(f.subCounty) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:county IS NULL OR f.county = :county) " +
           "AND (:status IS NULL OR f.status = :status) " +
           "AND (:farmerId IS NULL OR f.farmerId = :farmerId)")
    Page<Farm> searchFarms(@Param("search") String search,
                           @Param("county") String county,
                           @Param("status") com.kilivana.backend.common.enums.FarmStatus status,
                           @Param("farmerId") Long farmerId,
                           Pageable pageable);
}