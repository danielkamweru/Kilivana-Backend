package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.ecommerce.dto.CartResponse;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.repository.CartRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Cart getCartByBuyer(Long buyerId) {
        Optional<Cart> cart = cartRepository.findByBuyerIdAndStatus(buyerId, Cart.CartStatus.ACTIVE);
        if (cart.isEmpty()) {
            Cart newCart = Cart.builder()
                    .buyerId(buyerId)
                    .status(Cart.CartStatus.ACTIVE)
                    .build();
            return cartRepository.save(newCart);
        }
        return cart.get();
    }

    @Transactional
    public Cart addItemToCart(Long buyerId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Product product = orderableProduct(productId, quantity);

        Cart cart = getCartByBuyer(buyerId);
        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            // The snapshot is what the buyer agreed to, refreshed here because the price may
            // have moved since the line was first added.
            item.setPriceSnapshot(product.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .cartId(cart.getId())
                    .productId(productId)
                    .quantity(quantity)
                    .priceSnapshot(product.getPrice())
                    .build();
            cartItemRepository.save(item);
        }
        return cartRepository.save(cart);
    }

    private Product orderableProduct(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException(
                    "Product is not available for purchase: " + product.getStatus());
        }
        int stock = product.getStockQty() == null ? 0 : product.getStockQty();
        if (stock < quantity) {
            throw new BadRequestException("Only " + stock + " left in stock for product " + productId);
        }
        return product;
    }

    @Transactional
    public Cart updateCartItem(Long cartId, Long productId, Integer quantity) {
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", productId));
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        item.setQuantity(quantity);
        item.setPriceSnapshot(orderableProduct(productId, quantity).getPrice());
        cartItemRepository.save(item);
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
    }

    @Transactional
    public void removeCartItem(Long cartId, Long productId) {
        cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .ifPresent(cartItemRepository::delete);
    }

    @Transactional
    public void checkout(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
        if (cart.getStatus() != Cart.CartStatus.ACTIVE) {
            throw new BadRequestException("Cart is not active");
        }
        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        cart.setStatus(Cart.CartStatus.CHECKOUT_PROCESSING);
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long cartId) {
        cartItemRepository.findByCartId(cartId).forEach(cartItemRepository::delete);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
        cart.setStatus(Cart.CartStatus.ABANDONED);
        cartRepository.save(cart);
    }
}
