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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartCompatibilityController {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;

    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@RequestHeader("X-User-Id") Long buyerId) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(cartService.getCartByBuyer(buyerId))));
    }

    @PostMapping("/cart/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader("X-User-Id") Long buyerId,
            @RequestBody CartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(cartService.addItemToCart(buyerId, request.getProductId(), request.getQuantity()))));
    }

    @PutMapping("/cart/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long itemId, @RequestParam Integer quantity) {
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        return ResponseEntity.ok(ApiResponse.success(toResponse(
                cartService.updateCartItem(item.getCartId(), item.getProductId(), quantity))));
    }

    @DeleteMapping("/cart/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        cartService.removeCartItem(item.getCartId(), item.getProductId());
        return ResponseEntity.ok(ApiResponse.successMessage("Item removed from cart"));
    }

    @PostMapping("/checkout/validate")
    public ResponseEntity<ApiResponse<CartResponse>> validateCheckout(@RequestHeader("X-User-Id") Long buyerId) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(cartService.getCartByBuyer(buyerId))));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@RequestHeader("X-User-Id") Long buyerId) {
        Cart cart = cartService.getCartByBuyer(buyerId);
        cartService.checkout(cart.getId());
        return ResponseEntity.ok(ApiResponse.successMessage("Checkout successful"));
    }

    private CartResponse toResponse(Cart cart) {
        return CartResponse.fromEntity(cart);
    }
}