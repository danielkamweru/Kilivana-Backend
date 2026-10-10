package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.ecommerce.dto.CartItemRequest;
import com.kilivana.backend.ecommerce.dto.CartResponse;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.service.CartService;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Buyer cart and checkout compatibility endpoints.
 * Provides legacy-compatible paths under {@code /api/v1} for managing a buyer's cart
 * items and validating or completing a checkout. New implementations should use
 * {@link CartController} under {@code /api/v1/carts}.
 */
@Tag(name = "E-Commerce · Cart & Checkout", description = "Buyer cart items and checkout (compatibility)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartCompatibilityController {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;

    /**
     * Retrieves the current cart for the authenticated buyer.
     *
     * @param buyerId the authenticated buyer's user ID (injected via {@code @AuthenticationPrincipal})
     * @return the cart with all items and computed totals
     */
    @Operation(summary = "Get the buyer's cart",
            description = "Returns the current cart for the authenticated buyer, including all items and totals.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal Long buyerId) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(cartService.getCartByBuyer(buyerId))));
    }

    /**
     * Adds a product to the authenticated buyer's cart.
     * Creates the cart if it does not yet exist.
     *
     * @param buyerId the authenticated buyer's user ID
     * @param request the product ID and quantity to add
     * @return the updated cart
     */
    @Operation(summary = "Add an item to the cart",
            description = "Adds a product to the authenticated buyer's cart, creating the cart if it does not yet exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Item added to cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body or invalid quantity"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product already in cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/cart/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal Long buyerId,
            @RequestBody CartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(cartService.addItemToCart(buyerId, request.getProductId(), request.getQuantity()))));
    }

    /**
     * Updates the quantity of an existing cart item.
     * The cart item is identified by its item ID (not product ID).
     *
     * @param itemId the cart item ID
     * @param quantity the new quantity (must be positive)
     * @return the updated cart
     * @throws ResourceNotFoundException if the cart item does not exist
     */
    @Operation(summary = "Update a cart item quantity",
            description = "Changes the quantity of an existing cart item. Returns 404 if the item does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed request body or invalid quantity"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/cart/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @Parameter(description = "ID of the cart item to update") @PathVariable Long itemId,
            @Parameter(description = "New quantity for the cart item") @RequestParam Integer quantity) {
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        return ResponseEntity.ok(ApiResponse.success(toResponse(
                cartService.updateCartItem(item.getCartId(), item.getProductId(), quantity))));
    }

    /**
     * Removes a cart item by its item ID.
     *
     * @param itemId the cart item ID to remove
     * @return success message
     * @throws ResourceNotFoundException if the cart item does not exist
     */
    @Operation(summary = "Remove an item from the cart",
            description = "Removes a cart item by its ID. Returns 404 if the item does not exist.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item removed from cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/cart/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@Parameter(description = "ID of the cart item to remove") @PathVariable Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        cartService.removeCartItem(item.getCartId(), item.getProductId());
        return ResponseEntity.ok(ApiResponse.successMessage("Item removed from cart"));
    }

    /**
     * Validates the cart before checkout.
     * Returns the current cart state so the client can review totals.
     *
     * @param buyerId the authenticated buyer's user ID
     * @return the validated cart
     */
    @Operation(summary = "Validate checkout",
            description = "Validates the authenticated buyer's cart before checkout, returning the current cart state.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart validated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/checkout/validate")
    public ResponseEntity<ApiResponse<CartResponse>> validateCheckout(@AuthenticationPrincipal Long buyerId) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(cartService.getCartByBuyer(buyerId))));
    }

    /**
     * Completes the checkout for the authenticated buyer's cart.
     * Converts the cart into an order and clears the cart.
     *
     * @param buyerId the authenticated buyer's user ID
     * @return success message
     */
    @Operation(summary = "Checkout",
            description = "Completes the checkout for the authenticated buyer's cart.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cart already checked out or empty"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@AuthenticationPrincipal Long buyerId) {
        Cart cart = cartService.getCartByBuyer(buyerId);
        cartService.checkout(cart.getId());
        return ResponseEntity.ok(ApiResponse.successMessage("Checkout successful"));
    }

    /** Converts a Cart entity to its DTO representation. */
    private CartResponse toResponse(Cart cart) {
        return CartResponse.fromEntity(cart);
    }
}