package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.SupplierProfileImage;
import com.kilivana.backend.common.repository.ImageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProfileImageRepository extends JpaRepository<SupplierProfileImage, Long>, ImageRepository<SupplierProfileImage> {
    
    List<SupplierProfileImage> findByUserIdOrderBySortOrderAsc(Long userId);
    
    Optional<SupplierProfileImage> findByUserIdAndIsPrimaryTrue(Long userId);

    void deleteByUserId(Long userId);
}