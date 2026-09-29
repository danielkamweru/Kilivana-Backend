package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.DriverProfileImage;
import com.kilivana.backend.common.repository.ImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileImageRepository extends JpaRepository<DriverProfileImage, Long>, ImageRepository<DriverProfileImage> {
    
    List<DriverProfileImage> findByUserIdOrderBySortOrderAsc(Long userId);
    
    Optional<DriverProfileImage> findByUserIdAndIsPrimaryTrue(Long userId);

    void deleteByUserId(Long userId);
}