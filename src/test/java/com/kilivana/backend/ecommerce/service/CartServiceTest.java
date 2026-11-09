package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.repository.CartRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CartServiceTest {

    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final CartService service = new CartService(carts, items, products);

    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        cart = Cart.builder().id(10L).buyerId(7L).status(Cart.CartStatus.ACTIVE).build();
        product = Product.builder().id(20L).status(ProductStatus.ACTIVE)
                .stockQty(50).reservedQty(0).price(BigDecimal.TEN).build();
        when(carts.findByBuyerIdAndStatus(7L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(carts.findByBuyerIdAndStatusForUpdate(7L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(carts.findById(10L)).thenReturn(Optional.of(cart));
        when(carts.findByIdForUpdate(10L)).thenReturn(Optional.of(cart));
        when(carts.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.findByIdForUpdate(20L)).thenReturn(Optional.of(product));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addingToCartLeavesProductStockUnchanged() {
        when(items.findByCartIdAndProductId(10L, 20L)).thenReturn(Optional.empty());

        service.addItemToCart(7L, 20L, 3);

        assertEquals(50, product.getStockQty());
        assertEquals(0, product.getReservedQty());
        verify(items).save(argThat(item -> item.getQuantity() == 3));
        verify(products, never()).save(any(Product.class));
    }

    @Test
    void addingMoreThanCurrentStockIsRejected() {
        product.setStockQty(2);
        when(items.findByCartIdAndProductId(10L, 20L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.addItemToCart(7L, 20L, 3));

        assertEquals(2, product.getStockQty());
        verify(items, never()).save(any(CartItem.class));
    }

    @Test
    void updatingAndRemovingCartItemDoNotChangeProductStock() {
        CartItem item = CartItem.builder().id(30L).cartId(10L).productId(20L)
                .quantity(3).priceSnapshot(BigDecimal.TEN).build();
        when(items.findByCartIdAndProductId(10L, 20L)).thenReturn(Optional.of(item));

        service.updateCartItem(10L, 20L, 5);

        service.removeCartItem(10L, 20L);
        assertEquals(50, product.getStockQty());
        assertEquals(0, product.getReservedQty());
        verify(items).delete(item);
        verify(products, never()).save(any(Product.class));
    }
}
