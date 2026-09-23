package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.SupplierProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierProfileRepository extends JpaRepository<SupplierProfile, Long> {
    
    Optional<SupplierProfile> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
