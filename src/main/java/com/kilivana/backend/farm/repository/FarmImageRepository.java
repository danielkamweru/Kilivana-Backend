package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.FarmImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FarmImageRepository extends JpaRepository<FarmImage, Long> {
    List<FarmImage> findByFarmIdOrderBySortOrderAsc(Long farmId);

    java.util.Optional<FarmImage> findByFarmIdAndIsPrimaryTrue(Long farmId);
}
