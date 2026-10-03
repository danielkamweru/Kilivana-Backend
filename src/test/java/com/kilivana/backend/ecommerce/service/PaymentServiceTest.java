package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("a payment may only be recorded against an existing order")
    void refusesUnknownOrder() {
        when(orderRepository.findById(50L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.createPayment(50L, "mpesa", "QGH7X2", BigDecimal.TEN));
    }

    @Test
    @DisplayName("a payment is recorded in the panel's own spelling of the rail")
    void recordsKenyanRail() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(
                Order.builder().id(50L).build()));

        Payment payment = paymentService.createPayment(50L, "mpesa", "QGH7X2", BigDecimal.TEN);

        assertEquals(PaymentMethod.MPESA, payment.getMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    @DisplayName("the order's payment status follows its payment")
    void orderFollowsItsPayment() {
        Order order = Order.builder().id(50L).paymentStatus(PaymentStatus.PENDING).build();
        Payment payment = Payment.builder()
                .id(1L).orderId(50L).status(PaymentStatus.PENDING).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        Payment paid = paymentService.updatePaymentStatus(1L, PaymentStatus.PAID);

        assertEquals(PaymentStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPaidAt());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("paidAt is stamped once, on arrival")
    void paidAtStampedOnce() {
        Order order = Order.builder().id(50L).paymentStatus(PaymentStatus.PENDING).build();
        LocalDateTime firstArrival = LocalDateTime.now().minusMinutes(5);
        Payment payment = Payment.builder()
                .id(1L).orderId(50L).status(PaymentStatus.PAID)
                .paidAt(firstArrival).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        Payment settled = paymentService.updatePaymentStatus(1L, PaymentStatus.SETTLED);

        // The escrow steps after arrival must not re-stamp the arrival time.
        assertEquals(firstArrival, settled.getPaidAt());
        assertEquals(PaymentStatus.SETTLED, settled.getStatus());
    }
}
