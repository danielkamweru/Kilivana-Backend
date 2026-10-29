package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteResponse;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.repository.CartRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Locks in the pickup-delivery behaviour: the buyer types a one-time location, and
 * there is no platform station to select.
 */
class CheckoutServiceTest {

    private static final long BUYER_ID = 7L;
    private static final long CART_ID = 11L;
    private static final long PRODUCT_ID = 42L;

    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private AddressRepository addressRepository;
    private ProductRepository productRepository;
    private OrderService orderService;
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        addressRepository = mock(AddressRepository.class);
        productRepository = mock(ProductRepository.class);
        orderService = mock(OrderService.class);
        checkoutService = new CheckoutService(cartRepository, cartItemRepository, addressRepository,
                productRepository, orderService);

        // CurrentUser.id() reads the caller from the Spring SecurityContext, so the
        // test has to populate it the same way the JWT filter does.
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        BUYER_ID, null, List.of()));

        Cart cart = Cart.builder().id(CART_ID).buyerId(BUYER_ID)
                .status(Cart.CartStatus.CHECKOUT_PROCESSING).build();
        Product product = Product.builder()
                .id(PRODUCT_ID).sellerId(9L).name("Green Tea").unit("kg")
                .price(new BigDecimal("50.00")).status(ProductStatus.ACTIVE).build();
        CartItem item = CartItem.builder().id(1L).cartId(CART_ID)
                .productId(PRODUCT_ID).quantity(2).build();

        lenient().when(cartRepository.findFirstByBuyerIdAndStatus(BUYER_ID, Cart.CartStatus.CHECKOUT_PROCESSING))
                .thenReturn(Optional.of(cart));
        lenient().when(cartItemRepository.findByCartId(CART_ID)).thenReturn(List.of(item));
        lenient().when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pickupQuote_requires_a_typed_location() {
        DeliveryQuoteRequest request = DeliveryQuoteRequest.builder()
                .deliveryMethod("PICKUP_STATION").build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> checkoutService.createDeliveryQuote(request));
        assertEquals("Pickup location is required for pickup delivery", ex.getMessage());
    }

    @Test
    void pickupQuote_stores_only_the_typed_location() {
        DeliveryQuoteRequest request = DeliveryQuoteRequest.builder()
                .deliveryMethod("PICKUP_STATION")
                .pickupLocation("Outside Westlands Mall, near the main entrance")
                .build();

        DeliveryQuoteResponse response = checkoutService.createDeliveryQuote(request);

        assertNotNull(response.getQuoteId());
        assertEquals("PICKUP_STATION", response.getDeliveryMethod());
        assertEquals("Outside Westlands Mall, near the main entrance", response.getPickupLocation());
        assertEquals(new BigDecimal("1.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("101.00"), response.getTotal()); // 2 x 50.00 + 1.00
        assertEquals("KES", response.getCurrency());
    }

    @Test
    void pickupQuote_rejects_locations_over_500_characters() {
        DeliveryQuoteRequest request = DeliveryQuoteRequest.builder()
                .deliveryMethod("PICKUP_STATION")
                .pickupLocation("x".repeat(501))
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> checkoutService.createDeliveryQuote(request));
        assertEquals("Pickup location must be at most 500 characters", ex.getMessage());
    }
}