package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.dto.StkPushInitiateRequest;
import com.kilivana.backend.dto.StkPushInitiateResponse;
import com.kilivana.backend.dto.StkPushResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.service.DarajaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The checkout path: the buyer of an order pays for it with an M-PESA STK Push.
 *
 * <p>The buyer pays what the order owes, never what the client asks to pay: the
 * amount is the order's outstanding balance, computed on the server. A buyer may
 * only pay for their own order.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuyerStkPushTest {

    private static final Long BUYER_ID = 2L;
    private static final Long ORDER_ID = 50L;

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private DarajaService darajaService;
    @Mock private UserRepository userRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository, darajaService, userRepository);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(99L);
            return payment;
        });
    }

    private Order order() {
        return Order.builder()
                .id(ORDER_ID)
                .buyerId(BUYER_ID)
                .code("ORD-2851")
                .status(OrderStatus.PLACED)
                .total(new BigDecimal("1500.00"))
                .build();
    }

    private void givenOrder(Order order) {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    private void givenDarajaAccepts() {
        StkPushResponse response = new StkPushResponse();
        response.setMerchantRequestID("mr-1");
        response.setCheckoutRequestID("ws_CO_1");
        response.setResponseCode("0");
        when(darajaService.initiateStkPush(anyString(), anyInt(), anyString(), anyString())).thenReturn(response);
    }

    private StkPushInitiateRequest request(String phoneNumber) {
        StkPushInitiateRequest request = new StkPushInitiateRequest();
        request.setOrderId(ORDER_ID);
        request.setPhoneNumber(phoneNumber);
        return request;
    }

    @Test
    @DisplayName("the buyer pays the outstanding balance of their own order")
    void buyerPaysTheOutstandingBalance() {
        givenOrder(order());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());
        givenDarajaAccepts();

        StkPushInitiateResponse response = paymentService.initiateStkPushForBuyer(
                BUYER_ID, UserRole.BUYER, request("254712345678"));

        assertThat(response.getPaymentId()).isEqualTo(99L);
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(response.getStatus()).isEqualTo("pending");
        assertThat(response.getDarajaResponse().getCheckoutRequestID()).isEqualTo("ws_CO_1");
        // The amount sent to the provider is the order's balance, not a client figure.
        verify(darajaService).initiateStkPush(eq("254712345678"), eq(1500), eq("ORD-2851"), eq("Order ORD-2851"));
    }

    @Test
    @DisplayName("the amount is what is still owed, not the order total")
    void amountIsTheOutstandingBalance() {
        givenOrder(order());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(
                Payment.builder().id(1L).orderId(ORDER_ID).amount(new BigDecimal("500.00"))
                        .status(PaymentStatus.PAID).build()));
        givenDarajaAccepts();

        StkPushInitiateResponse response = paymentService.initiateStkPushForBuyer(
                BUYER_ID, UserRole.BUYER, request("254712345678"));

        assertThat(response.getAmount()).isEqualByComparingTo("1000.00");
        verify(darajaService).initiateStkPush(anyString(), eq(1000), anyString(), anyString());
    }

    @Test
    @DisplayName("the attempt is recorded as pending for the callback to reconcile")
    void attemptIsRecordedAsPending() {
        givenOrder(order());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());
        givenDarajaAccepts();

        paymentService.initiateStkPushForBuyer(BUYER_ID, UserRole.BUYER, request("254712345678"));

        var saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getValue().getMethod()).isEqualTo(PaymentMethod.MPESA);
        assertThat(saved.getValue().getReference()).isEqualTo("STK-ws_CO_1");
        assertThat(saved.getValue().getCheckoutRequestId()).isEqualTo("ws_CO_1");
        assertThat(saved.getValue().getPhoneNumber()).isEqualTo("254712345678");
        assertThat(saved.getValue().getCallbackStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("a buyer may not pay for somebody else's order")
    void buyerCannotPayForAnotherBuyersOrder() {
        givenOrder(order());

        assertThatThrownBy(() -> paymentService.initiateStkPushForBuyer(
                7L, UserRole.BUYER, request("254712345678")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You are not authorized to pay for this order");
        verify(darajaService, never()).initiateStkPush(anyString(), anyInt(), anyString(), anyString());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("an order that is already settled is not payable")
    void settledOrderIsRefused() {
        givenOrder(Order.builder().id(ORDER_ID).buyerId(BUYER_ID).code("ORD-2851")
                .status(OrderStatus.COMPLETED).total(new BigDecimal("1500.00")).build());

        assertThatThrownBy(() -> paymentService.initiateStkPushForBuyer(
                BUYER_ID, UserRole.BUYER, request("254712345678")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed");
        verify(darajaService, never()).initiateStkPush(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("an order with nothing owing is not payable")
    void noOutstandingBalanceIsRefused() {
        givenOrder(order());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(
                Payment.builder().id(1L).orderId(ORDER_ID).amount(new BigDecimal("1500.00"))
                        .status(PaymentStatus.PAID).build()));

        assertThatThrownBy(() -> paymentService.initiateStkPushForBuyer(
                BUYER_ID, UserRole.BUYER, request("254712345678")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No outstanding balance on this order");
        verify(darajaService, never()).initiateStkPush(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("a phone that is not a Kenyan mobile number is refused")
    void invalidPhoneIsRefused() {
        givenOrder(order());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> paymentService.initiateStkPushForBuyer(
                BUYER_ID, UserRole.BUYER, request("0712345678")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2547XXXXXXXX");
        verify(darajaService, never()).initiateStkPush(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("the buyer's registered phone is used when none is supplied")
    void registeredPhoneIsUsedAsFallback() {
        givenOrder(order());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());
        when(userRepository.findById(BUYER_ID)).thenReturn(Optional.of(
                User.builder().id(BUYER_ID).phone("254711111111").build()));
        givenDarajaAccepts();

        paymentService.initiateStkPushForBuyer(BUYER_ID, UserRole.BUYER, request(null));

        verify(darajaService).initiateStkPush(eq("254711111111"), eq(1500), anyString(), anyString());
    }

    @Test
    @DisplayName("a request without an order is refused")
    void missingOrderIdIsRefused() {
        StkPushInitiateRequest request = new StkPushInitiateRequest();

        assertThatThrownBy(() -> paymentService.initiateStkPushForBuyer(BUYER_ID, UserRole.BUYER, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order ID is required");
        verify(darajaService, never()).initiateStkPush(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("an unknown order is refused")
    void unknownOrderIsRefused() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiateStkPushForBuyer(
                BUYER_ID, UserRole.BUYER, request("254712345678")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("the seller's own endpoints still refuse a buyer")
    void buyerCannotUseTheSellerEndpoint() {
        givenOrder(order());

        assertThatThrownBy(() -> paymentService.initiateStkPush(BUYER_ID, UserRole.BUYER, request("254712345678")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You are not authorized to request payment for this order");
        verify(darajaService, never()).initiateStkPush(anyString(), anyInt(), anyString(), anyString());
    }
}
