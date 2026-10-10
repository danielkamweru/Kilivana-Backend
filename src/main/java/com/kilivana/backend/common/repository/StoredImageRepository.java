package com.kilivana.backend.common.repository;

import com.kilivana.backend.common.entity.StoredImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * CRUD access to images kept as bytes in PostgreSQL — the {@code database} storage
 * provider and Cloudinary's fallback.
 */
@Repository
public interface StoredImageRepository extends JpaRepository<StoredImage, Long> {
}