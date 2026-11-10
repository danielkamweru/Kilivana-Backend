package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.FarmerProfileRepository;
import com.kilivana.backend.admin.entity.FarmerProfile;
import com.kilivana.backend.admin.entity.SupplierProfile;
import com.kilivana.backend.admin.repository.SupplierProfileRepository;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.dto.OrderItemRequest;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.dto.SellerResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
    @Mock private PickupStationRepository pickupStationRepository;
    @Mock private FarmerProfileRepository farmerProfileRepository;
    @Mock private SupplierProfileRepository supplierProfileRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private LogisticsService logisticsService;
    @Mock private LogisticsJobRepository logisticsJobRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private com.kilivana.backend.admin.repository.SupplierWarehouseRepository warehouseRepository;

    private OrderService orderService;
    private Product product;

    private static final Long BUYER_ID = 7L;
    private static final Long ADDRESS_ID = 3L;
    private static final Long PRODUCT_ID = 42L;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository,
                orderEventRepository, paymentRepository, productRepository,
                userRepository, addressRepository, pickupStationRepository,
                farmerProfileRepository, supplierProfileRepository, driverProfileRepository,
                logisticsService, logisticsJobRepository,
                farmRepository, warehouseRepository);

        when(userRepository.existsById(BUYER_ID)).thenReturn(true);
        when(userRepository.findById(BUYER_ID)).thenReturn(Optional.of(User.builder().id(BUYER_ID).role(UserRole.BUYER).build()));
        when(userRepository.existsByIdAndRole(BUYER_ID, UserRole.BUYER)).thenReturn(true);
        Address address = Address.builder().id(ADDRESS_ID).userId(BUYER_ID).build();
        when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
        // The next order reference continues after the highest existing one.
        when(orderRepository.maxNumericOrderCode()).thenReturn(99);

        product = Product.builder()
                .id(PRODUCT_ID).sellerId(9L).name("Green Tea").unit("kg")
                .price(new BigDecimal("50.00"))
                .stockQty(10).reservedQty(0).soldQty(0).minimumOrderQty(1)
                .status(ProductStatus.ACTIVE).sellerType(SellerType.FARMER).farmId(3L).build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(farmRepository.findById(3L)).thenReturn(Optional.of(Farm.builder()
                .id(3L).farmerId(9L).status(FarmStatus.ACTIVE)
                .name("Test farm").address("Farm road").county("Nairobi")
                .latitude(-1.2).longitude(36.8).build()));

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
        lenient().when(orderRepository.findByIdForUpdate(anyLong()))
                .thenAnswer(inv -> orderRepository.findById(inv.getArgument(0)));
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
        assertEquals(8, product.getStockQty());
        assertEquals(2, product.getReservedQty());
        assertEquals(0, product.getSoldQty());
    }

    @Test
    @DisplayName("rejects products without an active dispatch location before saving an order")
    void rejectsProductWithoutDispatchLocation() {
        product.setFarmId(null);

        assertThrows(BadRequestException.class, () -> orderService.createOrder(BUYER_ID, ADDRESS_ID,
                new BigDecimal("25.00"), items(), null, null));

        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("creates an order when a farmer farm has no coordinates yet")
    void createsOrderWithFarmDispatchCoordinatesToBeAddedLater() {
        when(farmRepository.findById(3L)).thenReturn(Optional.of(Farm.builder()
                .id(3L).farmerId(9L).status(FarmStatus.ACTIVE)
                .name("Test farm").address("Farm road").build()));

        orderService.createOrder(BUYER_ID, ADDRESS_ID,
                new BigDecimal("25.00"), items(), null, null);

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.getDispatchLocationIdSnapshot().equals(3L)
                        && item.getDispatchLatitudeSnapshot() == null
                        && item.getDispatchLongitudeSnapshot() == null));
    }

    @Test
    @DisplayName("persists a buyer-entered pickup location without a station or address ID")
    void persistsManualPickupLocation() {
        String pickupLocation = "Outside Westlands Mall, near the main entrance";

        Order order = orderService.createOrder(BUYER_ID, null,
                new BigDecimal("25.00"), items(), null, null, "PICKUP_STATION", pickupLocation);

        assertEquals("PICKUP_STATION", order.getDeliveryMethod());
        assertEquals(pickupLocation, order.getPickupLocation());
        assertNull(order.getAddressId());
        verify(pickupStationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("rejects manual pickup locations longer than 500 characters")
    void rejectsOversizedManualPickupLocation() {
        String pickupLocation = "x".repeat(501);

        assertThrows(BadRequestException.class, () -> orderService.createOrder(BUYER_ID, null,
                new BigDecimal("25.00"), items(), null, null, "PICKUP_STATION", pickupLocation));
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
        when(userRepository.existsById(BUYER_ID)).thenReturn(true);
        when(userRepository.findById(BUYER_ID)).thenReturn(Optional.of(com.kilivana.backend.admin.entity.User.builder().id(BUYER_ID).role(UserRole.ADMIN).build()));
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
    @DisplayName("refuses more units than remain in stock")
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
    @DisplayName("a single farmer's details appear on the order and its items")
    void mapsSingleFarmerSellerDetailsForIndividualAndPagedOrderResponses() {
        Order order = responseOrder(200L);
        List<OrderItem> items = List.of(responseItem(1L, 9L, 42L));
        User farmer = User.builder().id(9L).name("John Kamau").phone("+254712345678")
                .region("Kiambu").role(UserRole.FARMER).build();
        when(orderItemRepository.findByOrderId(200L)).thenReturn(items);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of(farmer));
        when(farmerProfileRepository.findByUserIdIn(List.of(9L))).thenReturn(List.of(
                FarmerProfile.builder().userId(9L).location("Thika").build()));
        when(orderRepository.searchOrders(null, null, null, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(order)));

        OrderResponse detail = orderService.toResponse(order);
        OrderResponse listed = orderService.searchOrders(null, null, null, Pageable.unpaged())
                .map(orderService::toResponse).getContent().get(0);

        assertEquals("John Kamau", detail.getSellerName());
        assertEquals("+254712345678", detail.getSellerPhone());
        assertEquals("Kiambu", detail.getSellerCounty());
        assertEquals("Thika", detail.getSellerLocation());
        assertEquals(1, detail.getSellers().size());
        assertEquals("John Kamau", detail.getItems().get(0).getSellerName());
        assertEquals("Thika", detail.getItems().get(0).getSellerLocation());
        assertEquals(detail.getSellers(), listed.getSellers());
        assertEquals(detail.getItems(), listed.getItems());
    }

    @Test
    @DisplayName("a single supplier's profile location is returned")
    void mapsSingleSupplierSellerDetails() {
        Order order = responseOrder(201L);
        User supplier = User.builder().id(10L).name("Mary Wanjiku").phone("+254723456789")
                .region("Nakuru").role(UserRole.SUPPLIER).build();
        when(orderItemRepository.findByOrderId(201L)).thenReturn(List.of(responseItem(2L, 10L, 43L)));
        when(orderRepository.findById(201L)).thenReturn(Optional.of(order));
        when(userRepository.findAllById(List.of(10L))).thenReturn(List.of(supplier));
        when(supplierProfileRepository.findByUserIdIn(List.of(10L))).thenReturn(List.of(
                SupplierProfile.builder().userId(10L).location("Naivasha").build()));

        OrderResponse response = orderService.toResponse(order);

        assertEquals("Mary Wanjiku", response.getSellerName());
        assertEquals("Nakuru", response.getSellerCounty());
        assertEquals("Naivasha", response.getSellerLocation());
        assertEquals("Naivasha", response.getItems().get(0).getSellerLocation());
    }

    @Test
    @DisplayName("mixed and repeated seller lines return distinct sellers without a first-seller fallback")
    void mapsMultipleFarmersAndSuppliersWithoutDuplicates() {
        Order order = responseOrder(202L);
        List<OrderItem> items = List.of(
                responseItem(3L, 9L, 42L),
                responseItem(4L, 10L, 43L),
                responseItem(5L, 9L, 44L));
        User farmer = User.builder().id(9L).name("John Kamau").phone("+254712345678")
                .region("Kiambu").role(UserRole.FARMER).build();
        User supplier = User.builder().id(10L).name("Mary Wanjiku").phone("+254723456789")
                .region("Nakuru").role(UserRole.SUPPLIER).build();
        when(orderItemRepository.findByOrderId(202L)).thenReturn(items);
        when(orderRepository.findById(202L)).thenReturn(Optional.of(order));
        when(userRepository.findAllById(List.of(9L, 10L))).thenReturn(List.of(farmer, supplier));
        when(farmerProfileRepository.findByUserIdIn(List.of(9L, 10L))).thenReturn(List.of(
                FarmerProfile.builder().userId(9L).location("Thika").build()));
        when(supplierProfileRepository.findByUserIdIn(List.of(9L, 10L))).thenReturn(List.of(
                SupplierProfile.builder().userId(10L).location("Naivasha").build()));

        OrderResponse response = orderService.toResponse(order);

        assertNull(response.getSellerName());
        assertNull(response.getSellerPhone());
        assertNull(response.getSellerCounty());
        assertNull(response.getSellerLocation());
        assertEquals(2, response.getSellers().size());
        assertEquals(List.of(9L, 10L), response.getSellers().stream().map(SellerResponse::getSellerId).toList());
        assertEquals("Thika", response.getItems().get(0).getSellerLocation());
        assertEquals("Naivasha", response.getItems().get(1).getSellerLocation());
        assertEquals("John Kamau", response.getItems().get(2).getSellerName());
    }

    @Test
    @DisplayName("seller without a profile is returned without a location")
    void missingSellerProfileDoesNotBreakOrderResponse() {
        Order order = responseOrder(203L);
        User farmer = User.builder().id(9L).name("John Kamau").role(UserRole.FARMER).build();
        when(orderItemRepository.findByOrderId(203L)).thenReturn(List.of(responseItem(6L, 9L, 42L)));
        when(orderRepository.findById(203L)).thenReturn(Optional.of(order));
        when(userRepository.findAllById(List.of(9L))).thenReturn(List.of(farmer));

        OrderResponse response = orderService.toResponse(order);

        assertEquals(1, response.getSellers().size());
        assertNull(response.getSellerLocation());
        assertNull(response.getItems().get(0).getSellerLocation());
    }

    private static Order responseOrder(Long id) {
        return Order.builder().id(id).buyerId(BUYER_ID).status(OrderStatus.PLACED)
                .subtotal(BigDecimal.ZERO).deliveryFee(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .paymentStatus(PaymentStatus.PENDING).build();
    }

    private static OrderItem responseItem(Long id, Long sellerId, Long productId) {
        return OrderItem.builder().id(id).orderId(200L).productId(productId).sellerId(sellerId)
                .productName("Produce " + productId).unit("kg").quantity(1)
                .unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build();
    }

    @Test
    @DisplayName("cancelling an unpaid order releases its reserved stock")
    void cancelReleasesStock() {
        Order order = placedOrder();
        product.setStockQty(8);
        product.setReservedQty(2);
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(OrderItem.builder()
                .orderId(100L).productId(PRODUCT_ID).quantity(2).build()));

        Order cancelled = orderService.cancelOrder(100L);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        // Nothing had been paid, so the payment state is left alone.
        assertEquals(PaymentStatus.PENDING, cancelled.getPaymentStatus());
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertEquals(0, captor.getValue().getReservedQty());
        assertEquals(10, captor.getValue().getStockQty());
        assertEquals(0, product.getSoldQty());
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
        product.setStockQty(8);
        product.setReservedQty(2);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(delivered));
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(delivered));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(OrderItem.builder()
                .orderId(100L).productId(PRODUCT_ID).quantity(2).build()));
        when(paymentRepository.findByOrderId(100L)).thenReturn(List.of(
                com.kilivana.backend.ecommerce.entity.Payment.builder()
                        .id(1L).orderId(100L).build()));

        Order completed = orderService.confirmReceipt(100L);

        assertEquals(OrderStatus.COMPLETED, completed.getStatus());
        assertEquals(PaymentStatus.SETTLED, completed.getPaymentStatus());
        assertEquals(8, product.getStockQty());
        assertEquals(0, product.getReservedQty());
        assertEquals(2, product.getSoldQty());
        org.junit.jupiter.api.Assertions.assertTrue(completed.isSalesRecorded());

        assertEquals(completed, orderService.confirmReceipt(100L));
        assertEquals(2, product.getSoldQty());
    }

    @Test
    @DisplayName("delivery records multiple products and combines repeated historical order lines")
    void deliveryRecordsAggregatedQuantitiesForMultipleProducts() {
        Order delivered = placedOrder();
        delivered.setPaymentStatus(PaymentStatus.PAID);
        Product secondProduct = Product.builder()
                .id(43L).stockQty(6).reservedQty(4).soldQty(1).build();
        product.setStockQty(5);
        product.setReservedQty(5);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(delivered));
        when(productRepository.findByIdForUpdate(43L)).thenReturn(Optional.of(secondProduct));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(
                OrderItem.builder().orderId(100L).productId(PRODUCT_ID).quantity(2).build(),
                OrderItem.builder().orderId(100L).productId(PRODUCT_ID).quantity(3).build(),
                OrderItem.builder().orderId(100L).productId(43L).quantity(4).build()));

        orderService.updateOrderStatus(100L, OrderStatus.DELIVERED);

        assertEquals(5, product.getSoldQty());
        assertEquals(0, product.getReservedQty());
        assertEquals(5, product.getStockQty());
        assertEquals(5, secondProduct.getSoldQty());
        assertEquals(0, secondProduct.getReservedQty());
        assertEquals(6, secondProduct.getStockQty());
    }

    @Test
    @DisplayName("an unpaid or failed order cannot be completed as a sale")
    void refusesToCompleteUnpaidOrder() {
        Order delivered = placedOrder();
        delivered.setStatus(OrderStatus.DELIVERED);
        delivered.setPaymentStatus(PaymentStatus.FAILED);
        product.setStockQty(8);
        product.setReservedQty(2);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(delivered));

        assertThrows(BadRequestException.class, () -> orderService.confirmReceipt(100L));

        assertEquals(0, product.getSoldQty());
        assertEquals(2, product.getReservedQty());
        verify(productRepository, never()).save(any());

        delivered.setPaymentStatus(PaymentStatus.REFUNDED);
        assertThrows(BadRequestException.class, () -> orderService.confirmReceipt(100L));
        assertEquals(0, product.getSoldQty());
    }

    @Test
    @DisplayName("repeated product lines are combined before stock reservation")
    void combinesDuplicateLinesWhenCreatingOrder() {
        OrderItemRequest one = OrderItemRequest.builder()
                .productId(PRODUCT_ID).quantity(2).unitPrice(new BigDecimal("50.00")).build();
        OrderItemRequest two = OrderItemRequest.builder()
                .productId(PRODUCT_ID).quantity(2).unitPrice(new BigDecimal("50.00")).build();

        orderService.createOrder(BUYER_ID, ADDRESS_ID, BigDecimal.ZERO, List.of(one, two), null, null);

        assertEquals(6, product.getStockQty());
        assertEquals(4, product.getReservedQty());
        assertEquals(0, product.getSoldQty());
        ArgumentCaptor<OrderItem> items = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(items.capture());
        assertEquals(4, items.getValue().getQuantity());
    }

    @Test void manipulatedCatalogPriceRejectedBeforeReservation() {
        var request = items(); request.get(0).setUnitPrice(new BigDecimal("0.01"));
        assertThrows(BadRequestException.class, () -> orderService.createOrder(BUYER_ID, ADDRESS_ID, null, request, null, null));
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }
    @Test void invalidQuantitiesRejectedBeforeWrites() {
        var request = items(); request.get(0).setQuantity(0);
        assertThrows(BadRequestException.class, () -> orderService.createOrder(BUYER_ID, ADDRESS_ID, null, request, null, null));
        verify(productRepository, never()).save(any());
    }
    @Test void minimumOrderAndRemainingStockAreEnforced() {
        Product product = productRepository.findById(PRODUCT_ID).orElseThrow();
        product.setMinimumOrderQty(3);
        assertThrows(BadRequestException.class, () -> orderService.createOrder(BUYER_ID, ADDRESS_ID, null, items(), null, null));
        product.setMinimumOrderQty(1); product.setReservedQty(9);
        orderService.createOrder(BUYER_ID, ADDRESS_ID, null, items(), null, null);
        assertEquals(8, product.getStockQty());
        assertEquals(11, product.getReservedQty());
    }
    @Test void reservationUsesLockedCatalogRecord() {
        orderService.createOrder(BUYER_ID, ADDRESS_ID, null, items(), null, null);
        verify(productRepository).findByIdForUpdate(PRODUCT_ID);
    }
}
