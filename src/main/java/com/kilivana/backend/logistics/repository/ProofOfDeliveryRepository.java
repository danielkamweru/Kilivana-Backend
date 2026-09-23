package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {
    
    Optional<ProofOfDelivery> findByLogisticsJobId(Long logisticsJobId);
}
