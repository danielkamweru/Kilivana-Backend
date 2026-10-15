package com.kilivana.backend.common.repository;

import com.kilivana.backend.common.entity.BaseImageEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Image queries shared by every entity that extends {@link BaseImageEntity}.
 *
 * <p>The {@code #{#entityName}} JPQL template lets one interface serve all image tables:
 * each concrete repository extends this one and Spring substitutes its own entity name,
 * so product, driver and farm images all get the same lookups without duplication.
 */
public interface ImageRepository<T extends BaseImageEntity> {

    /** The owning entity's gallery, in display order. */
    List<T> findByResourceIdOrderBySortOrderAsc(Long resourceId);

    /** The entity's cover image, if one was marked primary. */
    @Query("SELECT i FROM #{#entityName} i WHERE i.resourceId = :resourceId AND i.isPrimary = true")
    Optional<T> findPrimaryByResourceId(@Param("resourceId") Long resourceId);

    /** Lookup by the client-facing identifier, as used by public image endpoints. */
    @Query("SELECT i FROM #{#entityName} i WHERE i.publicId = :publicId")
    Optional<T> findByPublicId(@Param("publicId") String publicId);
}
