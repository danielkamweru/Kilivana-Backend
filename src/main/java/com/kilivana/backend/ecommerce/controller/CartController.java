package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.CartItemRequest;
import com.kilivana.backend.ecommerce.dto.CartResponse;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.repository.CartRepository;
import com.kilivana.backend.ecommerce.service.CartService;
import com.kilivana.backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing buyer shopping carts.
 * Provides endpoints for retrieving cart contents, adding and updating items,
 * checkout and clearing. All operations are scoped to a specific buyer.
 */
@Tag(name = "E-Commerce · Cart & Checkout", description = "Buyer cart items and checkout")
@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    /**
     * Retrieves the active shopping cart for a specific buyer.
     *
     * @param buyerId the buyer's user ID
     * @return the cart with items and totals
     */
    @Operation(summary = "Get cart by buyer", description = "Retrieves the active shopping cart for a specific buyer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCartByBuyer(@Parameter(description = "Buyer identifier") @PathVariable Long buyerId) {
        Cart cart = cartService.getCartByBuyer(buyerId);
        CartResponse response = CartResponse.fromEntity(cart);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Adds a product item to the buyer's shopping cart.
     * Creates the cart if it does not exist.
     *
     * @param buyerId the buyer's user ID
     * @param request the product ID and quantity to add
     * @return the updated cart
     */
    @Operation(summary = "Add item to cart", description = "Adds a product item to the buyer's shopping cart and returns the updated cart.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Item added to cart successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Buyer or product not found")
    })
    @PostMapping("/buyer/{buyerId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(@Parameter(description = "Buyer identifier") @PathVariable Long buyerId, @Parameter(description = "Cart item to add") @RequestBody CartItemRequest request) {
        Cart cart = cartService.addItemToCart(buyerId, request.getProductId(), request.getQuantity());
        CartResponse response = CartResponse.fromEntity(cart);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Updates the quantity of a specific product in a cart.
     * The cart is identified by cartId, the product by productId.
     *
     * @param cartId the cart identifier
     * @param productId the product identifier
     * @param quantity the new quantity (must be positive)
     * @return the updated cart
     */
    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a specific product in a cart.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart or product not found")
    })
    @PutMapping("/{cartId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @Parameter(description = "Cart identifier") @PathVariable Long cartId,
            @Parameter(description = "Product identifier") @PathVariable Long productId,
            @Parameter(description = "New quantity for the cart item") @RequestParam Integer quantity) {
        Cart cart = cartService.updateCartItem(cartId, productId, quantity);
        CartResponse response = CartResponse.fromEntity(cart);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Removes a specific product from a shopping cart.
     *
     * @param cartId the cart identifier
     * @param productId the product identifier to remove
     * @return success message
     */
    @Operation(summary = "Remove cart item", description = "Removes a specific product from a shopping cart.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item removed from cart successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart or product not found")
    })
    @DeleteMapping("/{cartId}/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @Parameter(description = "Cart identifier") @PathVariable Long cartId,
            @Parameter(description = "Product identifier") @PathVariable Long productId) {
        cartService.removeCartItem(cartId, productId);
        return ResponseEntity.ok(ApiResponse.successMessage("Item removed from cart"));
    }

    /**
     * Processes the checkout for the given shopping cart, converting it into an order.
     *
     * @param cartId the cart identifier
     * @return success message
     */
    @Operation(summary = "Checkout cart", description = "Processes the checkout for the given shopping cart, converting it into an order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cart is empty or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@Parameter(description = "Cart identifier") @PathVariable Long cartId) {
        cartService.checkout(cartId);
        return ResponseEntity.ok(ApiResponse.successMessage("Checkout successful"));
    }

    /**
     * Removes all items from a shopping cart.
     *
     * @param cartId the cart identifier
     * @return success message
     */
    @Operation(summary = "Clear cart", description = "Removes all items from a shopping cart.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/{cartId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@Parameter(description = "Cart identifier") @PathVariable Long cartId) {
        cartService.clearCart(cartId);
        return ResponseEntity.ok(ApiResponse.successMessage("Cart cleared"));
    }
}
