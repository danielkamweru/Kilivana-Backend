package com.kilivana.backend.farm.repository;

import com.kilivana.backend.farm.entity.CropType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link CropType} catalog entities.
 * <p>
 * Crop types are reference data. The only query needed beyond CRUD is the
 * lookup of all active types ordered by name, which is used to populate
 * dropdown controls in the UI.
 */
@Repository
public interface CropTypeRepository extends JpaRepository<CropType, Long> {
    java.util.List<CropType> findByActiveTrueOrderByName();
}