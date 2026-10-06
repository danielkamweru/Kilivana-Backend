package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.FarmerProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.dto.OrderItemRequest;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.service.LogisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderEventRepository orderEventRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private FarmerProfileRepository farmerProfileRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private LogisticsService logisticsService;
    @Mock private LogisticsJobRepository logisticsJobRepository;

    private OrderService orderService;

    private static final Long BUYER_ID = 7L;
    private static final Long ADDRESS_ID = 3L;
    private static final Long PRODUCT_ID = 42L;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository,
                orderEventRepository, paymentRepository, productRepository,
                userRepository, addressRepository, farmerProfileRepository,
                driverProfileRepository, logisticsService, logisticsJobRepository);

        when(userRepository.existsByIdAndRole(BUYER_ID, UserRole.BUYER)).thenReturn(true);
        Address address = Address.builder().id(ADDRESS_ID).userId(BUYER_ID).build();
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
        // The next order reference continues after the highest existing one.
        when(orderRepository.maxNumericOrderCode()).thenReturn(99);

        Product product = Product.builder()
                .id(PRODUCT_ID).sellerId(9L).name("Green Tea").unit("kg")
                .price(new BigDecimal("50.00"))
                .stockQty(10).reservedQty(0).soldQty(0).minimumOrderQty(1)
                .status(ProductStatus.ACTIVE).build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // The first save assigns the identity, the second stores the code.
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(100L);
            }
            return order;
        });
        lenient().when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId(1L);
            }
            return item;
        });
        lenient().when(orderRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return id == 100L ? Optional.of(placedOrder()) : Optional.empty();
        });
    }

    private Order placedOrder() {
        return Order.builder()
                .id(100L).code("ORD-00100").buyerId(BUYER_ID)
                .status(OrderStatus.PLACED).paymentStatus(PaymentStatus.PENDING)
                .subtotal(new BigDecimal("100.00")).deliveryFee(new BigDecimal("25.00"))
                .total(new BigDecimal("125.00")).addressId(ADDRESS_ID).build();
    }

    private List<OrderItemRequest> items() {
        return List.of(OrderItemRequest.builder()
                .productId(PRODUCT_ID).quantity(2).unitPrice(new BigDecimal("50.00")).build());
    }

    @Test
    @DisplayName("computes the figures from the items and stores them")
    void computesTotalsFromItems() {
        Order order = orderService.createOrder(BUYER_ID, ADDRESS_ID,
                new BigDecimal("25.00"), items(), null, null);

        assertEquals(0, new BigDecimal("100.00").compareTo(order.getSubtotal()));
        assertEquals(0, new BigDecimal("125.00").compareTo(order.getTotal()));
        assertEquals("ORD-00100", order.getCode());
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    @DisplayName("persists the line items with the product's own name and unit")
    void persistsItemsWithProductDetails() {
        orderService.createOrder(BUYER_ID, ADDRESS_ID,
                new BigDecimal("25.00"), items(), null, null);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        OrderItem saved = captor.getValue();
        assertEquals(PRODUCT_ID, saved.getProductId());
        assertEquals(9L, saved.getSellerId());
        assertEquals("Green Tea", saved.getProductName());
        assertEquals("kg", saved.getUnit());
        assertEquals(2, saved.getQuantity());
        assertEquals(0, new BigDecimal("100.00").compareTo(saved.getSubtotal()));
    }

    @Test
    @DisplayName("reserves the ordered stock")
    void reservesStock() {
        orderService.createOrder(BUYER_ID, ADDRESS_ID,
                new BigDecimal("25.00"), items(), null, null);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertEquals(2, captor.getValue().getReservedQty());
    }

    @Test
    @DisplayName("refuses a basket whose caller-supplied figures disagree")
    void refusesDisagreeingFigures() {
        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(BUYER_ID, ADDRESS_ID, new BigDecimal("25.00"),
                        items(), new BigDecimal("90.00"), null));
        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(BUYER_ID, ADDRESS_ID, new BigDecimal("25.00"),
                        items(), null, new BigDecimal("999.00")));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("refuses a buyer that is not a buyer account")
    void refusesNonBuyer() {
        when(userRepository.existsByIdAndRole(BUYER_ID, UserRole.BUYER)).thenReturn(false);
        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(BUYER_ID, ADDRESS_ID,
                        new BigDecimal("25.00"), items(), null, null));
    }

    @Test
    @DisplayName("refuses an address that belongs to someone else")
    void refusesForeignAddress() {
        Address other = Address.builder().id(ADDRESS_ID).userId(8L).build();
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(other));
        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(BUYER_ID, ADDRESS_ID,
                        new BigDecimal("25.00"), items(), null, null));
    }

    @Test
    @DisplayName("refuses more units than are unreserved")
    void refusesOverselling() {
        List<OrderItemRequest> greedy = List.of(OrderItemRequest.builder()
                .productId(PRODUCT_ID).quantity(11).unitPrice(new BigDecimal("50.00")).build());
        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(BUYER_ID, ADDRESS_ID,
                        new BigDecimal("25.00"), greedy, null, null));
    }

    @Test
    @DisplayName("the response carries the order's line items")
    void responseIncludesItems() {
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(OrderItem.builder()
                .id(1L).orderId(100L).productId(PRODUCT_ID).sellerId(9L)
                .productName("Green Tea").unit("kg").quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00")).build()));

        OrderResponse response = orderService.toResponse(placedOrder());

        assertEquals("ORD-00100", response.getCode());
        assertEquals(1, response.getItems().size());
        assertEquals("Green Tea", response.getItems().get(0).getProductName());
        assertEquals("kg", response.getItems().get(0).getUnit());
    }

    @Test
    @DisplayName("cancelling an unpaid order releases its reserved stock")
    void cancelReleasesStock() {
        Order order = placedOrder();
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(OrderItem.builder()
                .orderId(100L).productId(PRODUCT_ID).quantity(2).build()));

        Order cancelled = orderService.cancelOrder(100L);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        // Nothing had been paid, so the payment state is left alone.
        assertEquals(PaymentStatus.PENDING, cancelled.getPaymentStatus());
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertEquals(0, captor.getValue().getReservedQty());
    }

    @Test
    @DisplayName("cancelling a paid order refunds it")
    void cancelRefundsPaidOrder() {
        Order order = placedOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of(
                com.kilivana.backend.ecommerce.entity.Payment.builder()
                        .id(1L).orderId(100L).build()));

        Order cancelled = orderService.cancelOrder(100L);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(PaymentStatus.REFUNDED, cancelled.getPaymentStatus());
        verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("only a delivered order can be completed")
    void confirmReceiptRequiresDelivery() {
        assertThrows(BadRequestException.class,
                () -> orderService.confirmReceipt(100L));
    }

    @Test
    @DisplayName("completing a delivered order settles its payment")
    void confirmReceiptSettlesPayment() {
        Order delivered = placedOrder();
        delivered.setStatus(OrderStatus.DELIVERED);
        delivered.setPaymentStatus(PaymentStatus.HELD);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(delivered));
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of(
                com.kilivana.backend.ecommerce.entity.Payment.builder()
                        .id(1L).orderId(100L).build()));

        Order completed = orderService.confirmReceipt(100L);

        assertEquals(OrderStatus.COMPLETED, completed.getStatus());
        assertEquals(PaymentStatus.SETTLED, completed.getPaymentStatus());
    }
}
