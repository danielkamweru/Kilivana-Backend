package com.kilivana.backend.service;

import com.kilivana.backend.config.DarajaProperties;
import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.dto.DarajaCallbackRequest;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.CallbackInboxRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DarajaCallbackService#timeoutPayment(Long)}: a STK Push
 * payment that remains PENDING past the local timeout deadline is marked FAILED,
 * and no order is created for pre-order payments.
 */
@ExtendWith(MockitoExtension.class)
class DarajaCallbackServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private com.kilivana.backend.service.DarajaService darajaService;
    @Mock private CallbackInboxRepository callbackInboxRepository;
    @Mock private DarajaProperties darajaProperties;

    @InjectMocks private DarajaCallbackService callbackService;

    @Test
    void timedOutPreOrderPaymentIsFailedAndNoOrderIsCreated() {
        Payment payment = Payment.builder()
                .id(7L).orderId(null).quoteId("QT-ABCD1234")
                .method(PaymentMethod.MPESA)
                .status(PaymentStatus.PENDING).build();
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));

        callbackService.timeoutPayment(7L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCallbackStatus()).isEqualTo("PROCESSED");
        assertThat(payment.getDarajaResultDesc()).contains("timed out");
        verify(paymentRepository, atLeastOnce()).save(payment);
        verify(orderRepository, never()).findById(anyLong());
        verify(orderService, never()).createOrderFromCheckoutQuote(anyString(), anyLong());
    }

    @Test
    void timedOutOrderLinkedPaymentFailsPaymentAndOrder() {
        Order order = Order.builder().id(50L).paymentStatus(PaymentStatus.PENDING).build();
        Payment payment = Payment.builder()
                .id(8L).orderId(50L).quoteId(null)
                .method(PaymentMethod.MPESA)
                .status(PaymentStatus.PENDING).build();
        when(paymentRepository.findById(8L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        callbackService.timeoutPayment(8L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderRepository).save(order);
    }

    @Test
    void timedOutAlreadySuccessfulOrderIsNotDowngraded() {
        Order order = Order.builder().id(50L).paymentStatus(PaymentStatus.PAID).build();
        Payment payment = Payment.builder()
                .id(9L).orderId(50L).quoteId(null)
                .method(PaymentMethod.MPESA)
                .status(PaymentStatus.PENDING).build();
        when(paymentRepository.findById(9L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        callbackService.timeoutPayment(9L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        // Order already PAID must not be downgraded to FAILED.
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void nonPendingPaymentIsNotChanged() {
        Payment payment = Payment.builder()
                .id(10L).orderId(null).quoteId(null)
                .method(PaymentMethod.MPESA)
                .status(PaymentStatus.PAID).build();
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        callbackService.timeoutPayment(10L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void cancelledPreOrderPaymentIsProcessedWithoutQueryingANullOrderId() {
        Payment payment = pendingQuotePayment(11L, "checkout-cancel", "merchant-cancel");
        when(paymentRepository.findByCheckoutRequestId("checkout-cancel")).thenReturn(Optional.of(payment));

        callbackService.processCallback(callback("checkout-cancel", "merchant-cancel", 1032,
                "Request cancelled by user"), "127.0.0.1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getCallbackStatus()).isEqualTo("PROCESSED");
        assertThat(payment.getDarajaResultCode()).isEqualTo(1032);
        verify(paymentRepository, atLeastOnce()).save(payment);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void failedPreOrderPaymentIsProcessedWithoutQueryingANullOrderId() {
        Payment payment = pendingQuotePayment(12L, "checkout-failed", "merchant-failed");
        when(paymentRepository.findByCheckoutRequestId("checkout-failed")).thenReturn(Optional.of(payment));

        callbackService.processCallback(callback("checkout-failed", "merchant-failed", 1,
                "The balance is insufficient for the transaction"), "127.0.0.1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getCallbackStatus()).isEqualTo("PROCESSED");
        assertThat(payment.getDarajaResultCode()).isEqualTo(1);
        verify(paymentRepository, atLeastOnce()).save(payment);
        verifyNoInteractions(orderRepository);
    }

    private static Payment pendingQuotePayment(Long id, String checkoutRequestId, String merchantRequestId) {
        return Payment.builder()
                .id(id).quoteId("quote-" + id).method(PaymentMethod.MPESA)
                .status(PaymentStatus.PENDING).amount(java.math.BigDecimal.valueOf(51))
                .checkoutRequestId(checkoutRequestId).merchantRequestId(merchantRequestId)
                .callbackStatus("PENDING")
                .build();
    }

    private static DarajaCallbackRequest callback(String checkoutRequestId, String merchantRequestId,
                                                   int resultCode, String resultDesc) {
        DarajaCallbackRequest.StkCallback stk = new DarajaCallbackRequest.StkCallback();
        stk.setCheckoutRequestID(checkoutRequestId);
        stk.setMerchantRequestID(merchantRequestId);
        stk.setResultCode(resultCode);
        stk.setResultDesc(resultDesc);
        DarajaCallbackRequest.BodyWrapper body = new DarajaCallbackRequest.BodyWrapper();
        body.setStkCallback(stk);
        DarajaCallbackRequest callback = new DarajaCallbackRequest();
        callback.setBody(body);
        return callback;
    }
}
