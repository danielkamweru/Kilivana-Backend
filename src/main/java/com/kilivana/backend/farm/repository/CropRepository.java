package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.Crop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {
    List<Crop> findByFarmId(Long farmId);

    Page<Crop> findByFarmerId(Long farmerId, Pageable pageable);

    @Query("SELECT c FROM Crop c WHERE " +
           "(:search IS NULL OR LOWER(c.variety) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:farmerId IS NULL OR c.farmerId = :farmerId) " +
           "AND (:farmId IS NULL OR c.farmId = :farmId) " +
           "AND (:cropTypeId IS NULL OR c.cropTypeId = :cropTypeId) " +
           "AND (:status IS NULL OR c.status = :status)")
    Page<Crop> searchCrops(@Param("search") String search,
                           @Param("farmerId") Long farmerId,
                           @Param("farmId") Long farmId,
                           @Param("cropTypeId") Long cropTypeId,
                           @Param("status") com.kilivana.backend.common.enums.CropStatus status,
                           Pageable pageable);
}
