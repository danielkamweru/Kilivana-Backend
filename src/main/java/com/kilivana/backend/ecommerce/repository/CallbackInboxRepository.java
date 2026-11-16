package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.CallbackInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CallbackInboxRepository extends JpaRepository<CallbackInbox, Long> {

    /** Find an unprocessed inbox entry by checkout request ID. */
    Optional<CallbackInbox> findByCheckoutRequestIdAndProcessedFalse(String checkoutRequestId);

    /** Find an unprocessed inbox entry by both IDs for idempotent matching. */
    Optional<CallbackInbox> findByCheckoutRequestIdAndMerchantRequestIdAndProcessedFalse(
            String checkoutRequestId, String merchantRequestId);
}
