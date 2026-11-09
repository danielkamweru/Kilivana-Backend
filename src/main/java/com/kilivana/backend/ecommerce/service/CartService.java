package com.kilivana.backend.ecommerce.service;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * A buyer's active shopping cart.
 *
 * <p>One active cart per buyer; adding an item creates it if missing. The cart stores a
 * price snapshot on each line so the buyer sees the price they agreed to at checkout even if
 * the product price changes later. Cart lines do not reserve inventory; stock is deducted
 * only when an order is successfully created during checkout.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    /**
     * Returns the buyer's active cart, creating one if none exists.
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.buyer(#buyerId)")
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

    /**
     * Adds a product to the buyer's cart, or increments quantity if already present.
     * Refreshes the price snapshot to the current product price.
     *
     * @throws BadRequestException if quantity is less than 1
     * @throws ResourceNotFoundException if product does not exist
     * @throws BadRequestException if product is not active or has insufficient stock
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.buyer(#buyerId)")
    public Cart addItemToCart(Long buyerId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Cart cart = getActiveCartForUpdate(buyerId);
        Product product = lockOrderableProduct(productId);

        Optional<CartItem> existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            int currentQuantity = item.getQuantity();
            int newQuantity = Math.addExact(currentQuantity, quantity);
            requireAvailableStock(product, newQuantity);
            item.setQuantity(newQuantity);
            // The snapshot is what the buyer agreed to, refreshed here because the price may
            // have moved since the line was first added.
            item.setPriceSnapshot(product.getPrice());
            cartItemRepository.save(item);
        } else {
            requireAvailableStock(product, quantity);
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

    private Product lockOrderableProduct(Long productId) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException(
                    "Product is not available for purchase: " + product.getStatus());
        }
        return product;
    }

    private static int availableStock(Product product) {
        int stock = product.getStockQty() == null ? 0 : product.getStockQty();
        if (stock < 0) {
            throw new BadRequestException("Invalid stock state for product " + product.getId());
        }
        return stock;
    }

    private static void requireAvailableStock(Product product, int quantity) {
        int available = availableStock(product);
        if (available < quantity) {
            throw new BadRequestException(
                    "Only " + available + " left in stock for product " + product.getId());
        }
    }

    /**
     * Updates the quantity of a cart item and refreshes its price snapshot.
     *
     * @throws ResourceNotFoundException if the cart item does not exist
     * @throws BadRequestException if quantity is less than 1 or product has insufficient stock
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.cart(#cartId)")
    public Cart updateCartItem(Long cartId, Long productId, Integer quantity) {
        requireActiveCartForUpdate(cartId);
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException(
                    "Product is not available for purchase: " + product.getStatus());
        }
        requireAvailableStock(product, quantity);
        item.setQuantity(quantity);
        item.setPriceSnapshot(product.getPrice());
        cartItemRepository.save(item);
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
    }

    /**
     * Removes a product from a cart. No-op if the item is not present.
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.cart(#cartId)")
    public void removeCartItem(Long cartId, Long productId) {
        requireActiveCartForUpdate(cartId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId).orElse(null);
        if (item == null) {
            return;
        }
        cartItemRepository.delete(item);
    }

    /**
     * Flips the cart to {@code CHECKOUT_PROCESSING} so the order service can
     * read its lines and create the order. No further mutations are allowed
     * once the cart enters this state.
     *
     * @throws ResourceNotFoundException if cart does not exist
     * @throws BadRequestException if cart is not active or is empty
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.cart(#cartId)")
    public void checkout(Long cartId) {
        Cart cart = cartRepository.findByIdForUpdate(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));

        if (cart.getStatus() != Cart.CartStatus.ACTIVE) {
            throw new BadRequestException("Cart is not active and cannot be checked out");
        }

        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        if (items.isEmpty()) {
            throw new BadRequestException("Cannot checkout an empty cart");
        }

        // Change cart status to CHECKOUT_PROCESSING
        cart.setStatus(Cart.CartStatus.CHECKOUT_PROCESSING);
        cartRepository.save(cart);
    }

    /**
     * Clears all items from a cart and marks it {@code ABANDONED} (a terminal
     * state kept for analytics but never reactivated).
     *
     * @throws ResourceNotFoundException if cart does not exist
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.cart(#cartId)")
    public void clearCart(Long cartId) {
        Cart cart = requireActiveCartForUpdate(cartId);
        cartItemRepository.deleteAll(cartItemRepository.findByCartId(cartId));
        // Abandoned is a terminal state: the cart is kept for analytics but
        // will never be reactivated.
        cart.setStatus(Cart.CartStatus.ABANDONED);
        cartRepository.save(cart);
    }

    private Cart getActiveCartForUpdate(Long buyerId) {
        return cartRepository.findByBuyerIdAndStatusForUpdate(buyerId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .buyerId(buyerId)
                        .status(Cart.CartStatus.ACTIVE)
                        .build()));
    }

    private Cart requireActiveCartForUpdate(Long cartId) {
        Cart cart = cartRepository.findByIdForUpdate(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
        if (cart.getStatus() != Cart.CartStatus.ACTIVE) {
            throw new BadRequestException("Cart is not active and cannot be changed");
        }
        return cart;
    }
}
