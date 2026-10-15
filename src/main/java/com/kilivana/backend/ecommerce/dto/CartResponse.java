package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.ecommerce.entity.Cart;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response representing a shopping cart.
 *
 * <p>Contains the cart identity, the buyer who owns it, its current lifecycle
 * status, and timestamps. The items themselves are fetched separately via the
 * cart item endpoints to keep this response lightweight for list views.
 */
@Schema(description = "Shopping cart response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    @Schema(description = "Cart ID", example = "10")
    private Long id;

    @Schema(description = "Buyer user ID", example = "42")
    private Long buyerId;

    @Schema(description = "Current cart status", example = "ACTIVE")
    private Cart.CartStatus status;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T11:45:00")
    private LocalDateTime updatedAt;

    /**
     * Maps a {@link Cart} entity to this DTO.
     *
     * @param cart the cart entity to convert
     * @return a populated CartResponse
     */
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
