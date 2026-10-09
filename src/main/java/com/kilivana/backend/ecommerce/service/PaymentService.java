package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.ecommerce.dto.PaymentResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
    @RequiredArgsConstructor
    public class PaymentService {

    /**
     * Records payment attempts and mirrors their status onto the parent order.
     *
     * <p>The order's {@code paymentStatus} is a derived view of its payment rows — it is updated
     * here so the two never drift. A payment method is validated against the panel's enum so a
     * caller cannot store a provider string that nothing will ever read back.
     */

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    /**
     * @throws BadRequestException when the method is not one the panel can send, rather
     *     than storing a provider string nothing will ever read back
     * @throws ResourceNotFoundException when the order the payment is
     *     for does not exist
     */
    public Payment createPayment(Long orderId, String method, String reference, BigDecimal amount) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        PaymentMethod paymentMethod = PaymentMethod.from(method);
        if (paymentMethod == null) {
            throw new BadRequestException("Unknown payment method '" + method
                    + "'. Expected one of: MPESA, BANK or CARD.");
        }
        Payment payment = Payment.builder()
                .orderId(orderId)
                .method(paymentMethod)
                .reference(reference)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();
        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    public List<Payment> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Transactional
    public Payment updatePaymentStatus(Long id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        payment.setStatus(status);
        // paidAt records when the money actually arrived, so the escrow steps that
        // follow PAID must not stamp it again.
        if (status == PaymentStatus.PAID && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }
        Payment saved = paymentRepository.save(payment);
        // The order's payment status is a view of its payments, so it follows
        // the payment rather than being set separately and drifting.
        orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
            order.setPaymentStatus(status);
            orderRepository.save(order);
        });
        return saved;
    }

    public PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getMethod(),
                payment.getReference(), payment.getAmount(), payment.getStatus(),
                payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
