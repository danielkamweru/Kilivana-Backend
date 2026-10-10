package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.InspectorProfileImage;
import com.kilivana.backend.common.repository.ImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Access to the {@link InspectorProfileImage} table, which extends the shared image fields defined
 * in {@link ImageRepository}. Images are ordered by sort order so the panel can render them in a
 * stable sequence, and one may be flagged primary for the profile avatar.
 */
@Repository
public interface InspectorProfileImageRepository extends JpaRepository<InspectorProfileImage, Long>, ImageRepository<InspectorProfileImage> {

    List<InspectorProfileImage> findByUserIdOrderBySortOrderAsc(Long userId);

    Optional<InspectorProfileImage> findByUserIdAndIsPrimaryTrue(Long userId);

    void deleteByUserId(Long userId);
}
