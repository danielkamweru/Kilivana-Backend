package com.kilivana.backend.common.repository;

import com.kilivana.backend.common.entity.BaseImageEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImageRepository<T extends BaseImageEntity> {

    List<T> findByResourceIdOrderBySortOrderAsc(Long resourceId);

    @Query("SELECT i FROM #{#entityName} i WHERE i.resourceId = :resourceId AND i.isPrimary = true")
    Optional<T> findPrimaryByResourceId(@Param("resourceId") Long resourceId);

    @Query("SELECT i FROM #{#entityName} i WHERE i.publicId = :publicId")
    Optional<T> findByPublicId(@Param("publicId") String publicId);
}
