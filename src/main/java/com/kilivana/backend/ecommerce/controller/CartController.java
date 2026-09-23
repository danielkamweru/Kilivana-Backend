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

@RestController
@RequestMapping("/api/v1/ecommerce/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCartByBuyer(@PathVariable Long buyerId) {
        Cart cart = cartService.getCartByBuyer(buyerId);
        CartResponse response = CartResponse.fromEntity(cart);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/buyer/{buyerId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(@PathVariable Long buyerId, @RequestBody CartItemRequest request) {
        Cart cart = cartService.addItemToCart(buyerId, request.getProductId(), request.getQuantity());
        CartResponse response = CartResponse.fromEntity(cart);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{cartId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(@PathVariable Long cartId, @PathVariable Long productId, @RequestParam Integer quantity) {
        Cart cart = cartService.updateCartItem(cartId, productId, quantity);
        CartResponse response = CartResponse.fromEntity(cart);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{cartId}/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(@PathVariable Long cartId, @PathVariable Long productId) {
        cartService.removeCartItem(cartId, productId);
        return ResponseEntity.ok(ApiResponse.successMessage("Item removed from cart"));
    }

    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@PathVariable Long cartId) {
        cartService.checkout(cartId);
        return ResponseEntity.ok(ApiResponse.successMessage("Checkout successful"));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Long cartId) {
        cartService.clearCart(cartId);
        return ResponseEntity.ok(ApiResponse.successMessage("Cart cleared"));
    }
}
