package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.ecommerce.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Persistence for order disputes.
 */
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    /** Returns all disputes for a specific order. */
    List<Dispute> findByOrderId(Long orderId);

    /** Returns all disputes raised by a specific user. */
    List<Dispute> findByRaisedBy(Long raisedBy);

    /** Returns dispute counts grouped by the user who raised them. */
    @Query("SELECT d.raisedBy, COUNT(d) FROM Dispute d GROUP BY d.raisedBy")
    List<Object[]> countByBuyerId();

    /** Returns disputes filtered by status. */
    List<Dispute> findByStatus(DisputeStatus status);

    /** Returns count of disputes whose status is in the given list. */
    long countByStatusIn(List<DisputeStatus> statuses);
}
