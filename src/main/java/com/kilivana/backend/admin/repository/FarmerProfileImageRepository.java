package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.FarmerProfileImage;
import com.kilivana.backend.common.repository.ImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerProfileImageRepository extends JpaRepository<FarmerProfileImage, Long>, ImageRepository<FarmerProfileImage> {
    
    List<FarmerProfileImage> findByUserIdOrderBySortOrderAsc(Long userId);
    
    Optional<FarmerProfileImage> findByUserIdAndIsPrimaryTrue(Long userId);

    void deleteByUserId(Long userId);
}