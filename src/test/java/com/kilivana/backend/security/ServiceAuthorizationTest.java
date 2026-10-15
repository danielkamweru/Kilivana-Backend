package com.kilivana.backend.security;

import com.kilivana.backend.admin.controller.AddressController;
import com.kilivana.backend.admin.controller.InspectionController;
import com.kilivana.backend.admin.entity.*;
import com.kilivana.backend.admin.repository.*;
import com.kilivana.backend.admin.service.*;
import com.kilivana.backend.admin.dto.AddressRequest;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.exception.GlobalExceptionHandler;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.ecommerce.dto.*;
import com.kilivana.backend.ecommerce.entity.*;
import com.kilivana.backend.ecommerce.repository.*;
import com.kilivana.backend.ecommerce.service.*;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.service.LogisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actual Spring service proxies; repository mocks cannot turn a denied call into a write. */
@SpringJUnitConfig(ServiceAuthorizationTest.Config.class)
class ServiceAuthorizationTest {
    @Configuration @EnableMethodSecurity
    @Import({ResourceAuthorization.class, InspectionService.class, OrderService.class,
            PaymentService.class, DisputeService.class, CartService.class, AddressService.class,
            NotificationService.class, ProductService.class, UserService.class, LogisticsService.class})
    static class Config {
        @Bean com.kilivana.backend.logistics.service.TrackingStopService trackingStop() { return mock(com.kilivana.backend.logistics.service.TrackingStopService.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean InspectionRepository inspections() { return mock(InspectionRepository.class); }
        @Bean InspectionEvidenceImageRepository evidence() { return mock(InspectionEvidenceImageRepository.class); }
        @Bean OrderRepository orders() { return mock(OrderRepository.class); }
        @Bean OrderItemRepository items() { return mock(OrderItemRepository.class); }
        @Bean OrderEventRepository events() { return mock(OrderEventRepository.class); }
        @Bean com.kilivana.backend.service.DarajaService daraja() { return mock(com.kilivana.backend.service.DarajaService.class); }
        @Bean PaymentRepository payments() { return mock(PaymentRepository.class); }
        @Bean DisputeRepository disputes() { return mock(DisputeRepository.class); }
        @Bean ProductRepository products() { return mock(ProductRepository.class); }
        @Bean ProductImageRepository productImages() { return mock(ProductImageRepository.class); }
        @Bean CategoryRepository categories() { return mock(CategoryRepository.class); }
        @Bean CartRepository carts() { return mock(CartRepository.class); }
        @Bean CartItemRepository cartItems() { return mock(CartItemRepository.class); }
        @Bean AddressRepository addresses() { return mock(AddressRepository.class); }
        @Bean FarmerProfileRepository farmers() { return mock(FarmerProfileRepository.class); }
        @Bean DriverProfileRepository drivers() { return mock(DriverProfileRepository.class); }
        @Bean LogisticsJobRepository jobs() { return mock(LogisticsJobRepository.class); }
        @Bean com.kilivana.backend.logistics.repository.TrackingEventRepository tracking() { return mock(com.kilivana.backend.logistics.repository.TrackingEventRepository.class); }
        @Bean com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository proofs() { return mock(com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository.class); }
        @Bean com.kilivana.backend.mail.DeliveryOtpNotifier notifier() { return mock(com.kilivana.backend.mail.DeliveryOtpNotifier.class); }
        @Bean NotificationRepository notifications() { return mock(NotificationRepository.class); }
        @Bean ImageStorage storage() { return mock(ImageStorage.class); }
        @Bean org.springframework.security.crypto.password.PasswordEncoder encoder() { return mock(org.springframework.security.crypto.password.PasswordEncoder.class); }
        @Bean com.kilivana.backend.common.service.UserReferenceCodeGenerator references() { return mock(com.kilivana.backend.common.service.UserReferenceCodeGenerator.class); }
    }
    @Autowired UserRepository users;
    @Autowired InspectionRepository inspections;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired PaymentRepository payments;
    @Autowired DisputeRepository disputes;
    @Autowired CartRepository carts;
    @Autowired NotificationRepository notifications;
    @Autowired AddressRepository addresses;
    @Autowired ProductRepository products;
    @Autowired InspectionService inspectionService;
    @Autowired OrderService orderService;
    @Autowired PaymentService paymentService;
    @Autowired DisputeService disputeService;
    @Autowired CartService cartService;
    @Autowired AddressService addressService;
    @Autowired NotificationService notificationService;
    @Autowired ProductService productService;
    @Autowired UserService userService;
    @Autowired LogisticsService logistics;
    @Autowired LogisticsJobRepository jobs;

    @BeforeEach void fixture() {
        reset(users, inspections, orders, orderItems, payments, disputes, carts, notifications, addresses, products, jobs);
        actor(1L, UserRole.BUYER);
        when(inspections.findById(20L)).thenReturn(Optional.of(Inspection.builder().id(20L).inspectorId(3L).build()));
        when(orders.findById(10L)).thenReturn(Optional.of(Order.builder().id(10L).buyerId(1L).build()));
        when(orders.findById(11L)).thenReturn(Optional.of(Order.builder().id(11L).buyerId(2L).build()));
        when(payments.findById(7L)).thenReturn(Optional.of(Payment.builder().id(7L).orderId(11L).build()));
        when(disputes.findById(6L)).thenReturn(Optional.of(Dispute.builder().id(6L).raisedBy(2L).orderId(11L).build()));
        when(carts.findById(9L)).thenReturn(Optional.of(Cart.builder().id(9L).buyerId(2L).build()));
        when(notifications.findById(8L)).thenReturn(Optional.of(Notification.builder().id(8L).userId(2L).build()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void actor(Long id, UserRole role) {
        when(users.findById(id)).thenReturn(Optional.of(User.builder().id(id).role(role).status(UserStatus.ACTIVE).build()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of()));
    }
    void denied(org.junit.jupiter.api.function.Executable action) { assertThrows(AccessDeniedException.class, action); }

    @Test void pendingFarmerKeepsOwnAccessButCannotEscalateProductStatus() {
        actor(4L, UserRole.FARMER);
        when(users.findById(4L)).thenReturn(Optional.of(User.builder().id(4L).role(UserRole.FARMER)
                .status(UserStatus.PENDING_VERIFICATION).build()));
        assertEquals(4L, userService.getUserById(4L).getId());
        denied(() -> productService.updateProductStatusWithReason(1L, ProductStatus.ACTIVE, "test", 4L));
        verify(products, never()).findByIdForUpdate(any());
    }
    @Test void ownedSellerStkReachesUpstreamBalanceValidation() {
        actor(4L, UserRole.FARMER);
        var line = OrderItem.builder().sellerId(4L).orderId(11L).build();
        when(orderItems.findByOrderId(11L)).thenReturn(List.of(line));
        when(orders.findById(11L)).thenReturn(Optional.of(Order.builder().id(11L).buyerId(2L)
                .status(OrderStatus.PLACED).total(java.math.BigDecimal.ZERO).orderItems(List.of(line)).build()));
        var request = new com.kilivana.backend.dto.StkPushInitiateRequest(); request.setOrderId(11L);
        var error = assertThrows(com.kilivana.backend.common.exception.BadRequestException.class,
                () -> paymentService.initiateStkPush(4L, UserRole.FARMER, request));
        assertEquals("No outstanding balance on this order", error.getMessage());
        verify(payments, never()).save(any());
    }

    @Test void stkCannotTrustSuppliedIdentityOrUnrelatedOrder() {
        var request = new com.kilivana.backend.dto.StkPushInitiateRequest();
        request.setOrderId(11L);
        actor(4L, UserRole.FARMER);
        denied(() -> paymentService.initiateStkPush(2L, UserRole.FARMER, request));
        denied(() -> paymentService.initiateStkPush(4L, UserRole.FARMER, request));
        verify(payments, never()).save(any());
    }

    @Test void inspectionRequiresAssignedInspectorOrAdmin() {
        denied(() -> inspectionService.getInspectionById(20L));
        actor(4L, UserRole.INSPECTOR);
        denied(() -> inspectionService.updateInspectionStatus(20L, InspectionStatus.COMPLETED));
        denied(() -> inspectionService.listEvidenceImages(20L));
        denied(() -> inspectionService.deleteInspection(20L));
        denied(() -> inspectionService.getInspectionsByInspector(3L));
        verify(inspections, never()).save(any());
        actor(3L, UserRole.INSPECTOR);
        assertEquals(20L, inspectionService.getInspectionById(20L).getId());
    }
    @ParameterizedTest @ValueSource(strings = {"/api/v1/inspections/20", "/api/v1/admin/inspections/20"})
    void inspectionAliasesReturn403(String path) throws Exception {
        MockMvcBuilders.standaloneSetup(new InspectionController(inspectionService))
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get(path)).andExpect(status().isForbidden());
    }
    @Test void orderIsolationAndBuyerPlacementUnavailable() {
        denied(() -> orderService.getOrderById(11L));
        denied(() -> orderService.getOrdersByBuyer(2L));
        denied(() -> orderService.searchOrders(null, null, org.springframework.data.domain.Pageable.unpaged()));
        denied(() -> orderService.confirmReceipt(11L));
        denied(() -> orderService.updateOrderStatus(10L, OrderStatus.COMPLETED));
        denied(() -> orderService.createOrder(1L, 1L, null, List.of(), null, null));
        verify(orders, never()).save(any());
        assertEquals(10L, orderService.getOrderById(10L).getId());
    }
    @Test void sellerCannotReadAnotherSellersMixedOrder() {
        actor(4L, UserRole.FARMER);
        denied(() -> orderService.getOrderById(11L));
        denied(() -> orderService.getOrderItems(11L));
    }
    @Test void paymentOperationsRequireAdminAndReadsOwnBuyer() {
        denied(() -> paymentService.getPaymentById(7L));
        denied(() -> paymentService.getPaymentsByOrder(11L));
        denied(() -> paymentService.createPayment(10L, "MPESA", "test", java.math.BigDecimal.ONE));
        denied(() -> paymentService.updatePaymentStatus(7L, PaymentStatus.REFUNDED));
        verify(payments, never()).save(any());
    }
    @Test void disputeImpersonationAndResolutionDenied() {
        denied(() -> disputeService.getDisputeById(6L));
        denied(() -> disputeService.getDisputesByUser(2L));
        denied(() -> disputeService.createDispute(DisputeRequest.builder().orderId(11L).raisedBy(2L).build()));
        denied(() -> disputeService.updateDisputeStatus(6L, DisputeStatus.RESOLVED));
        denied(() -> disputeService.resolveDispute(6L, new DisputeResolutionRequest()));
        verify(disputes, never()).save(any());
    }
    @Test void cartIsolationAndNoFakeCheckout() {
        denied(() -> cartService.getCartByBuyer(2L));
        denied(() -> cartService.addItemToCart(2L, 1L, 1));
        denied(() -> cartService.updateCartItem(9L, 1L, 1));
        denied(() -> cartService.removeCartItem(9L, 1L));
        denied(() -> cartService.clearCart(9L));
        denied(() -> cartService.checkout(9L));
        when(carts.findById(9L)).thenReturn(Optional.of(Cart.builder().id(9L).buyerId(1L).build()));
        assertThrows(ServiceUnavailableException.class, () -> cartService.checkout(9L));
        verify(carts, never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings = {"/api/v1/users/2/addresses", "/api/v1/2/addresses"})
    void addressAliasesCannotTrustPathUser(String path) throws Exception {
        MockMvcBuilders.standaloneSetup(new AddressController(addressService))
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get(path)).andExpect(status().isForbidden());
    }
    @Test void addressMutationsCannotImpersonateOwner() {
        denied(() -> addressService.createAddress(2L, new AddressRequest()));
        denied(() -> addressService.updateAddress(2L, 1L, new AddressRequest()));
        denied(() -> addressService.deleteAddress(2L, 1L));
        verifyNoInteractions(addresses);
        assertTrue(addressService.getAddressesByUser(1L).isEmpty());
    }
    @Test void notificationIsolationAcrossReadWriteAndFeed() {
        denied(() -> notificationService.getNotificationById(8L));
        denied(() -> notificationService.getNotificationsByUserId(2L));
        denied(() -> notificationService.getAllNotifications());
        denied(() -> notificationService.markAsRead(8L));
        denied(() -> notificationService.markAllAsRead(2L));
        denied(() -> notificationService.deleteNotification(8L));
        verify(notifications, never()).save(any());
    }
    @Test void ownersCannotEscalateProductModeration() {
        actor(4L, UserRole.FARMER);
        denied(() -> productService.updateProductStatus(42L, ProductStatus.ACTIVE));
        verifyNoInteractions(products);
    }
    @Test void legitimateAdminStillReadsAcrossOwners() {
        actor(99L, UserRole.ADMIN);
        assertEquals(20L, inspectionService.getInspectionById(20L).getId());
        assertEquals(11L, orderService.getOrderById(11L).getId());
        assertEquals(7L, paymentService.getPaymentById(7L).getId());
        assertEquals(6L, disputeService.getDisputeById(6L).getId());
        assertEquals(8L, notificationService.getNotificationById(8L).getId());
        assertTrue(addressService.getAddressesByUser(2L).isEmpty());
    }

    @Test void privateAccountReadRequiresSelfOrAdmin() {
        denied(() -> userService.getUserById(2L));
        assertEquals(1L, userService.getUserById(1L).getId());
    }

    @Test void buyerCannotCreateFarmerProducts() {
        denied(() -> productService.createProduct(new ProductRequest(), 1L));
        verifyNoInteractions(products);
    }
    @Test void adminCanModerateButPendingCreationPolicyIsUnchanged() {
        actor(99L, UserRole.ADMIN);
        var p = Product.builder().id(42L).stockQty(10).status(ProductStatus.PENDING_APPROVAL).build();
        when(products.findByIdForUpdate(42L)).thenReturn(Optional.of(p));
        when(products.save(any())).thenAnswer(i -> i.getArgument(0));
        productService.updateProductStatus(42L, ProductStatus.ACTIVE);
        assertEquals(ProductStatus.ACTIVE, p.getStatus());
    }

    @Test void logisticsCannotBypassOrderOwnership() {
        when(jobs.findById(50L)).thenReturn(Optional.of(com.kilivana.backend.logistics.entity.LogisticsJob.builder().id(50L).orderId(11L).driverId(5L).build()));
        denied(() -> logistics.getJobById(50L));
        denied(() -> logistics.updateJobStatus(50L, DeliveryStatus.DELIVERED));
        denied(() -> logistics.verifyDeliveryOtp(50L, "000000"));
        actor(4L, UserRole.DRIVER);
        denied(() -> logistics.acceptJob(50L, 5L));
        actor(5L, UserRole.DRIVER);
        assertEquals(50L, logistics.getJobById(50L).getId());
        actor(99L, UserRole.ADMIN);
        assertEquals(50L, logistics.getJobById(50L).getId());
    }
}
