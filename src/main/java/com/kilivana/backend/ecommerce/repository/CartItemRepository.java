package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    /** Finds a specific line in a cart by its product. */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
