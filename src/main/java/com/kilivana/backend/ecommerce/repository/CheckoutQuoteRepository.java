package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.CheckoutQuote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckoutQuoteRepository extends JpaRepository<CheckoutQuote, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM CheckoutQuote q WHERE q.quoteId = :quoteId")
    Optional<CheckoutQuote> findByQuoteIdForUpdate(@Param("quoteId") String quoteId);
}
