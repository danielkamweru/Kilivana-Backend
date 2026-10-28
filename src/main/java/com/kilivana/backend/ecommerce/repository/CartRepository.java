package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Finds the cart for a buyer in a specific state (typically {@code ACTIVE}).
     * At most one active cart exists per buyer.
     */
    Optional<Cart> findByBuyerIdAndStatus(Long buyerId, Cart.CartStatus status);

    /** Returns any one cart for the buyer in the given state (useful when duplicates exist). */
    Optional<Cart> findFirstByBuyerIdAndStatus(Long buyerId, Cart.CartStatus status);
}
