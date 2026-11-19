package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.ecommerce.dto.CheckoutOrderRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteResponse;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.entity.CheckoutQuote;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.dto.StkPushInitiateResponse;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.repository.CartRepository;
import com.kilivana.backend.ecommerce.repository.CheckoutQuoteRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import com.kilivana.backend.ecommerce.service.PaymentService;
import com.kilivana.backend.logistics.entity.PickupStation;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

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
    private CheckoutQuoteRepository checkoutQuoteRepository;
    private OrderService orderService;
    private PaymentService paymentService;
    private PickupStationRepository pickupStationRepository;
    private CheckoutService checkoutService;
    private final Map<String, CheckoutQuote> savedQuotes = new HashMap<>();

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        addressRepository = mock(AddressRepository.class);
        productRepository = mock(ProductRepository.class);
        checkoutQuoteRepository = mock(CheckoutQuoteRepository.class);
        orderService = mock(OrderService.class);
        paymentService = mock(PaymentService.class);
        pickupStationRepository = mock(PickupStationRepository.class);
        savedQuotes.clear();
        checkoutService = new CheckoutService(cartRepository, cartItemRepository, addressRepository,
                productRepository, orderService, paymentService, pickupStationRepository, checkoutQuoteRepository);
        when(checkoutQuoteRepository.save(any(CheckoutQuote.class))).thenAnswer(invocation -> {
            CheckoutQuote quote = invocation.getArgument(0);
            savedQuotes.put(quote.getQuoteId(), quote);
            return quote;
        });
        when(checkoutQuoteRepository.findByQuoteIdForUpdate(any())).thenAnswer(invocation ->
                Optional.ofNullable(savedQuotes.get(invocation.getArgument(0))));

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

        lenient().when(cartRepository.findTopByBuyerIdAndStatusOrderByIdDesc(BUYER_ID, Cart.CartStatus.CHECKOUT_PROCESSING))
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
    void doorDeliveryQuote_acceptsAddressWithoutCoordinates() {
        Address address = Address.builder()
                .id(8L)
                .userId(BUYER_ID)
                .addressText("Mamlaka Road")
                .build();
        when(addressRepository.findById(8L)).thenReturn(Optional.of(address));

        DeliveryQuoteResponse response = checkoutService.createDeliveryQuote(DeliveryQuoteRequest.builder()
                .deliveryMethod("DOOR_DELIVERY")
                .addressId(8L)
                .build());

        assertNotNull(response.getQuoteId());
        assertEquals("DOOR_DELIVERY", response.getDeliveryMethod());
        assertEquals(8L, response.getAddressId());
        assertEquals(new BigDecimal("101.00"), response.getTotal());
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

    @Test
    void pickupQuote_resolvesCanonicalAvailableStation() {
        PickupStation station = PickupStation.builder()
                .id(5L).name("Westlands Hub").address("Canonical address")
                .latitude(-1.28).longitude(36.81).region("Nairobi").isAvailable(true).build();
        when(pickupStationRepository.findById(5L)).thenReturn(Optional.of(station));

        DeliveryQuoteResponse response = checkoutService.createDeliveryQuote(DeliveryQuoteRequest.builder()
                .deliveryMethod("PICKUP_STATION").pickupStationId(5L).build());

        assertEquals(5L, response.getPickupStationId());
        assertEquals("Canonical address", response.getPickupStation().getAddress());
        assertEquals(-1.28, response.getPickupStation().getLatitude());
    }

    @Test
    void repeatedOrderRequest_returnsTheOriginalOrderWithoutReinitiatingPayment() {
        String pickupLocation = "Outside Westlands Mall, near the main entrance";
        DeliveryQuoteResponse quote = checkoutService.createDeliveryQuote(DeliveryQuoteRequest.builder()
                .deliveryMethod("PICKUP_STATION")
                .pickupLocation(pickupLocation)
                .build());
        Cart cart = Cart.builder().id(CART_ID).buyerId(BUYER_ID)
                .status(Cart.CartStatus.CHECKOUT_PROCESSING).build();
        when(cartRepository.findByIdForUpdate(CART_ID)).thenReturn(Optional.of(cart));

        // Simulate a previous successful payment: the quote is now used and linked to order 99.
        CheckoutQuote stored = savedQuotes.get(quote.getQuoteId());
        stored.setUsed(true);
        stored.setOrderId(99L);
        when(orderService.getOrderById(99L))
                .thenReturn(Order.builder().id(99L).total(new BigDecimal("101.00")).build());

        CheckoutOrderRequest request = CheckoutOrderRequest.builder().quoteId(quote.getQuoteId()).build();
        StkPushInitiateResponse response = checkoutService.createOrderFromQuote(request);

        assertEquals(99L, response.getOrderId());
        assertEquals("PAID", response.getStatus());
        assertEquals(new BigDecimal("101.00"), response.getAmount());
        verify(paymentService, never()).initiateStkPushForQuote(any(Long.class), anyString(), anyString());
    }

    @Test
    void createOrderFromQuote_initiatesPaymentInsteadOfCreatingOrder() {
        String pickupLocation = "Outside Westlands Mall, near the main entrance";
        DeliveryQuoteResponse quote = checkoutService.createDeliveryQuote(DeliveryQuoteRequest.builder()
                .deliveryMethod("PICKUP_STATION")
                .pickupLocation(pickupLocation)
                .build());
        Cart cart = Cart.builder().id(CART_ID).buyerId(BUYER_ID)
                .status(Cart.CartStatus.CHECKOUT_PROCESSING).build();
        when(cartRepository.findByIdForUpdate(CART_ID)).thenReturn(Optional.of(cart));

        StkPushInitiateResponse stkResponse = new StkPushInitiateResponse();
        stkResponse.setPaymentId(77L);
        stkResponse.setOrderId(null);
        stkResponse.setAmount(new BigDecimal("101.00"));
        stkResponse.setStatus("PENDING");
        when(paymentService.initiateStkPushForQuote(eq(BUYER_ID), eq(quote.getQuoteId()), isNull()))
                .thenReturn(stkResponse);

        CheckoutOrderRequest request = CheckoutOrderRequest.builder().quoteId(quote.getQuoteId()).build();
        StkPushInitiateResponse response = checkoutService.createOrderFromQuote(request);

        assertEquals(77L, response.getPaymentId());
        assertNull(response.getOrderId());
        assertEquals("PENDING", response.getStatus());
        verify(orderService, never()).createOrder(any(), any(), any(), anyList(), any(), any(), any(), any(), any());
    }

    @Test
    void deliveryQuote_includesAllCartProductsInSubtotalAndTotal() {
        // Three distinct products: 30 + 40 + 30 = 100 subtotal, + 1 delivery = 101 total
        long product1Id = 101L;
        long product2Id = 102L;
        long product3Id = 103L;

        Product p1 = Product.builder().id(product1Id).sellerId(9L).name("Maize").unit("kg")
                .price(new BigDecimal("30.00")).status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(product2Id).sellerId(9L).name("Beans").unit("kg")
                .price(new BigDecimal("40.00")).status(ProductStatus.ACTIVE).build();
        Product p3 = Product.builder().id(product3Id).sellerId(9L).name("Rice").unit("kg")
                .price(new BigDecimal("30.00")).status(ProductStatus.ACTIVE).build();

        CartItem i1 = CartItem.builder().id(1L).cartId(CART_ID).productId(product1Id).quantity(1).build();
        CartItem i2 = CartItem.builder().id(2L).cartId(CART_ID).productId(product2Id).quantity(1).build();
        CartItem i3 = CartItem.builder().id(3L).cartId(CART_ID).productId(product3Id).quantity(1).build();

        lenient().when(cartItemRepository.findByCartId(CART_ID)).thenReturn(List.of(i1, i2, i3));
        lenient().when(productRepository.findById(product1Id)).thenReturn(Optional.of(p1));
        lenient().when(productRepository.findById(product2Id)).thenReturn(Optional.of(p2));
        lenient().when(productRepository.findById(product3Id)).thenReturn(Optional.of(p3));

        DeliveryQuoteResponse response = checkoutService.createDeliveryQuote(DeliveryQuoteRequest.builder()
                .deliveryMethod("PICKUP_STATION")
                .pickupLocation("Outside Westlands Mall, near the main entrance")
                .build());

        assertNotNull(response.getQuoteId());
        assertEquals(new BigDecimal("100.00"), response.getSubtotal());
        assertEquals(new BigDecimal("1.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("101.00"), response.getTotal());
        assertEquals("KES", response.getCurrency());
    }
}