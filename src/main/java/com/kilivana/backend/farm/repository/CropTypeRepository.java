package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.CropType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CropTypeRepository extends JpaRepository<CropType, Long> {
    java.util.List<CropType> findByActiveTrueOrderByName();
}
