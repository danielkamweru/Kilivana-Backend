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

    /**
     * Cast optional parameters so PostgreSQL can evaluate an unfiltered list
     * request instead of rejecting its null parameters as untyped.
     */
    @Query(value = "SELECT * FROM farms f WHERE " +
           "(CAST(:search AS VARCHAR) IS NULL OR LOWER(f.name) LIKE LOWER('%' || :search || '%') " +
           " OR LOWER(f.county) LIKE LOWER('%' || :search || '%') " +
           " OR LOWER(f.sub_county) LIKE LOWER('%' || :search || '%')) " +
           "AND (CAST(:county AS VARCHAR) IS NULL OR f.county = :county) " +
           "AND (CAST(:status AS VARCHAR) IS NULL OR f.status = CAST(:status AS VARCHAR)) " +
           "AND (CAST(:farmerId AS BIGINT) IS NULL OR f.farmer_id = :farmerId)",
           countQuery = "SELECT COUNT(*) FROM farms f WHERE " +
           "(CAST(:search AS VARCHAR) IS NULL OR LOWER(f.name) LIKE LOWER('%' || :search || '%') " +
           " OR LOWER(f.county) LIKE LOWER('%' || :search || '%') " +
           " OR LOWER(f.sub_county) LIKE LOWER('%' || :search || '%')) " +
           "AND (CAST(:county AS VARCHAR) IS NULL OR f.county = :county) " +
           "AND (CAST(:status AS VARCHAR) IS NULL OR f.status = CAST(:status AS VARCHAR)) " +
           "AND (CAST(:farmerId AS BIGINT) IS NULL OR f.farmer_id = :farmerId)",
           nativeQuery = true)
    Page<Farm> searchFarms(@Param("search") String search,
                           @Param("county") String county,
                           @Param("status") com.kilivana.backend.common.enums.FarmStatus status,
                           @Param("farmerId") Long farmerId,
                           Pageable pageable);
}
