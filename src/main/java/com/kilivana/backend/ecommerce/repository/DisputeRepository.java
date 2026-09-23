package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.ecommerce.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    
    List<Dispute> findByOrderId(Long orderId);
    
    List<Dispute> findByRaisedBy(Long raisedBy);
    
    List<Dispute> findByStatus(DisputeStatus status);
}
