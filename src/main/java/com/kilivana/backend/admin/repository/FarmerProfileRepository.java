package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.FarmerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {
    
    Optional<FarmerProfile> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
