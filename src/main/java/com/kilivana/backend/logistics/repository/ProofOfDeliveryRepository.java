package com.kilivana.backend.logistics.repository;

import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * One proof of delivery per job. The proof is the record that a delivery was handed over;
 * it is filed once, after the driver has confirmed the customer's handover code.
 */
@Repository
public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {

    /** The proof filed for a delivery job, or empty when none has been filed yet. */
    Optional<ProofOfDelivery> findByLogisticsJobId(Long logisticsJobId);

    /** True when a proof has already been filed for this job. */
    boolean existsByLogisticsJobId(Long logisticsJobId);
}