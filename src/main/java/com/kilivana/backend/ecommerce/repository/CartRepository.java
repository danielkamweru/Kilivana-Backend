package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Finds the cart for a buyer in a specific state (typically {@code ACTIVE}).
     * At most one active cart exists per buyer.
     */
    Optional<Cart> findByBuyerIdAndStatus(Long buyerId, Cart.CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.buyerId = :buyerId AND c.status = :status")
    Optional<Cart> findByBuyerIdAndStatusForUpdate(@Param("buyerId") Long buyerId,
                                                    @Param("status") Cart.CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.id = :id")
    Optional<Cart> findByIdForUpdate(@Param("id") Long id);

    /** Returns any one cart for the buyer in the given state (useful when duplicates exist). */
    Optional<Cart> findFirstByBuyerIdAndStatus(Long buyerId, Cart.CartStatus status);
}
