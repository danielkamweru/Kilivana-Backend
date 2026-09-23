package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.common.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogisticsJobRepository extends JpaRepository<LogisticsJob, Long> {
    
    List<LogisticsJob> findByOrderId(Long orderId);
    
    List<LogisticsJob> findByDriverId(Long driverId);
    
    List<LogisticsJob> findByStatus(DeliveryStatus status);
    
    List<LogisticsJob> findByDriverIdAndStatus(Long driverId, DeliveryStatus status);
}
