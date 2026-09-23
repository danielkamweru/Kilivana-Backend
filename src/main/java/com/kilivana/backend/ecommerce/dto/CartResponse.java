package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.ecommerce.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long buyerId;
    private Cart.CartStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartResponse fromEntity(Cart cart) {
        return CartResponse.builder()
                .id(cart.getId())
                .buyerId(cart.getBuyerId())
                .status(cart.getStatus())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
