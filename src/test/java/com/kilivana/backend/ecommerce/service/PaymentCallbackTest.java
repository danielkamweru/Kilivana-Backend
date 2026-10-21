package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives {@link PaymentService} through the Daraja callback reconciliation paths:
 * an accepted STK Push must not mark the order paid; only a validated successful
 * result does. paidAt must be stamped exactly once, on arrival.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCallbackTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private com.kilivana.backend.service.DarajaService darajaService;
    @Mock private com.kilivana.backend.admin.repository.UserRepository userRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository, darajaService, userRepository);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void anAcceptedStkPushDoesNotMarkTheOrderPaid() {
        Order order = Order.builder().id(50L).paymentStatus(PaymentStatus.PENDING).build();
        Payment payment = Payment.builder()
                .id(1L).orderId(50L).method(PaymentMethod.MPESA)
                .reference("CHECKOUT_1").status(PaymentStatus.PENDING).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        Payment result = paymentService.updatePaymentStatus(1L, PaymentStatus.PENDING);

        // An accepted STK Push (PENDING) must not mark the order paid: the order's
        // payment status stays PENDING and no arrival time is stamped.
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void onlyASuccessfulResultMarksTheOrderPaidAndStampsPaidAt() {
        Order order = Order.builder().id(50L).paymentStatus(PaymentStatus.PENDING).build();
        Payment payment = Payment.builder()
                .id(1L).orderId(50L).method(PaymentMethod.MPESA)
                .reference("CHECKOUT_1").status(PaymentStatus.PENDING).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        Payment result = paymentService.updatePaymentStatus(1L, PaymentStatus.PAID);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void paidAtIsNotReplacedWhenMovingToSettled() {
        Order order = Order.builder().id(50L).paymentStatus(PaymentStatus.PENDING).build();
        LocalDateTime arrival = LocalDateTime.now().minusMinutes(5);
        Payment payment = Payment.builder()
                .id(1L).orderId(50L).method(PaymentMethod.MPESA)
                .reference("CHECKOUT_1").status(PaymentStatus.PAID)
                .paidAt(arrival).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        Payment settled = paymentService.updatePaymentStatus(1L, PaymentStatus.SETTLED);

        assertThat(settled.getPaidAt()).isEqualTo(arrival);
        assertThat(settled.getStatus()).isEqualTo(PaymentStatus.SETTLED);
    }
}