package com.kilivana.backend.common.repository;

import com.kilivana.backend.common.entity.StoredImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredImageRepository extends JpaRepository<StoredImage, Long> {
}