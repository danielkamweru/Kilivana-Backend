package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.dto.DisputeRequest;
import com.kilivana.backend.ecommerce.dto.DisputeResolutionRequest;
import com.kilivana.backend.ecommerce.dto.DisputeResolutionRequest.DisputeOutcome;
import com.kilivana.backend.ecommerce.entity.Dispute;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.DisputeRepository;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventRepository orderEventRepository;
    @Mock private PaymentRepository paymentRepository;

    private DisputeService disputeService;

    private static final Long ORDER_ID = 50L;
    private static final Long DISPUTE_ID = 5L;

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(disputeRepository, orderRepository,
                orderEventRepository, paymentRepository);

        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
            Dispute dispute = inv.getArgument(0);
            if (dispute.getId() == null) {
                dispute.setId(DISPUTE_ID);
            }
            return dispute;
        });
        lenient().when(orderEventRepository.save(any(OrderEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Order paidOrder() {
        return Order.builder()
                .id(ORDER_ID).buyerId(7L)
                .status(OrderStatus.DELIVERED).paymentStatus(PaymentStatus.PAID)
                .subtotal(new BigDecimal("100.00")).deliveryFee(new BigDecimal("25.00"))
                .total(new BigDecimal("125.00")).addressId(3L).build();
    }

    private DisputeRequest raise() {
        return DisputeRequest.builder()
                .orderId(ORDER_ID).raisedBy(7L)
                .reason("Wrong items")
                .description("The crate contained tomatoes, not tea").build();
    }

    @Test
    @DisplayName("raising a dispute marks the order disputed and holds the money")
    void raisingHoldsOrderAndPayment() {
        Order order = paidOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(
                Payment.builder().id(1L).orderId(ORDER_ID).status(PaymentStatus.PAID).build()));

        disputeService.createDispute(raise());

        assertEquals(OrderStatus.DISPUTED, order.getStatus());
        assertEquals(PaymentStatus.HELD, order.getPaymentStatus());
        ArgumentCaptor<OrderEvent> events = ArgumentCaptor.forClass(OrderEvent.class);
        org.mockito.Mockito.verify(orderEventRepository, org.mockito.Mockito.atLeastOnce()).save(events.capture());
        assertEquals(OrderStatus.DISPUTED, events.getAllValues().get(0).getStatus());
    }

    @Test
    @DisplayName("raising a dispute against an unknown order is refused")
    void refusesUnknownOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> disputeService.createDispute(raise()));
    }

    @Test
    @DisplayName("an unpaid order is disputed but holds nothing")
    void unpaidOrderStaysPending() {
        Order order = paidOrder();
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        disputeService.createDispute(raise());

        assertEquals(OrderStatus.DISPUTED, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    private Dispute openDispute() {
        return Dispute.builder()
                .id(DISPUTE_ID).orderId(ORDER_ID).raisedBy(7L)
                .reason("Wrong items").status(DisputeStatus.OPEN).build();
    }

    @Test
    @DisplayName("resolving for the buyer cancels the order and refunds the money")
    void buyerFavourCancelsAndRefunds() {
        Order order = paidOrder();
        order.setStatus(OrderStatus.DISPUTED);
        order.setPaymentStatus(PaymentStatus.HELD);
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(openDispute()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(
                Payment.builder().id(1L).orderId(ORDER_ID).status(PaymentStatus.HELD).build()));

        disputeService.resolveDispute(DISPUTE_ID, DisputeResolutionRequest.builder()
                .outcome(DisputeOutcome.BUYER).resolution("Refund issued").build());

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(PaymentStatus.REFUNDED, order.getPaymentStatus());
        ArgumentCaptor<Dispute> disputes = ArgumentCaptor.forClass(Dispute.class);
        org.mockito.Mockito.verify(disputeRepository).save(disputes.capture());
        assertEquals(DisputeStatus.RESOLVED, disputes.getValue().getStatus());
        assertEquals("Refund issued", disputes.getValue().getResolution());
    }

    @Test
    @DisplayName("resolving for the farmer completes the order and releases the money")
    void farmerFavourCompletesAndSettles() {
        Order order = paidOrder();
        order.setStatus(OrderStatus.DISPUTED);
        order.setPaymentStatus(PaymentStatus.HELD);
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(openDispute()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());

        disputeService.resolveDispute(DISPUTE_ID, DisputeResolutionRequest.builder()
                .outcome(DisputeOutcome.FARMER).resolution("Delivered after all").build());

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(PaymentStatus.SETTLED, order.getPaymentStatus());
    }

    @Test
    @DisplayName("a resolved dispute cannot be resolved twice")
    void refusesDoubleResolution() {
        Dispute resolved = openDispute();
        resolved.setStatus(DisputeStatus.RESOLVED);
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(resolved));

        assertThrows(BadRequestException.class, () ->
                disputeService.resolveDispute(DISPUTE_ID, DisputeResolutionRequest.builder()
                        .outcome(DisputeOutcome.BUYER).resolution("Again").build()));
    }
}
