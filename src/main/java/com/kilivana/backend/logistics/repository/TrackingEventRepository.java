package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    
    List<TrackingEvent> findByLogisticsJobIdOrderByRecordedAtDesc(Long logisticsJobId);
    
    List<TrackingEvent> findByDriverId(Long driverId);
}
