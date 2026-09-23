package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.InspectorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspectorProfileRepository extends JpaRepository<InspectorProfile, Long> {
    
    Optional<InspectorProfile> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
