package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.dto.DisputeRequest;
import com.kilivana.backend.ecommerce.dto.DisputeResolutionRequest;
import com.kilivana.backend.ecommerce.dto.DisputeResponse;
import com.kilivana.backend.ecommerce.dto.DisputeResolutionRequest.DisputeOutcome;
import com.kilivana.backend.ecommerce.entity.Dispute;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.DisputeRepository;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The dispute lifecycle, as the panel runs it.
 *
 * <p>Raising a dispute marks the order disputed and holds the money:
 * the buyer has questioned the delivery, so the payment leaves the
 * "paid" state and waits. Resolving it settles the question either
 * way - the buyer's favour cancels the order and refunds the money,
 * the farmer's favour completes the order and releases it to the
 * seller - and records why.
 */
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentRepository paymentRepository;

    /**
     * @throws BadRequestException when the order the dispute is
     *     raised against does not exist
     */
    @Transactional
    public DisputeResponse createDispute(DisputeRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));

        Dispute dispute = Dispute.builder()
                .orderId(request.getOrderId())
                .raisedBy(request.getRaisedBy())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : DisputeStatus.OPEN)
                .build();
        Dispute saved = disputeRepository.save(dispute);

        holdOrder(order);
        return mapToResponse(saved);
    }

    public DisputeResponse getDisputeById(Long id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
        return mapToResponse(dispute);
    }

    public List<DisputeResponse> getDisputesByOrder(Long orderId) {
        return disputeRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DisputeResponse> getDisputesByUser(Long userId) {
        return disputeRepository.findByRaisedBy(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DisputeResponse updateDisputeStatus(Long id, DisputeStatus status) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
        dispute.setStatus(status);
        return mapToResponse(disputeRepository.save(dispute));
    }

    /**
     * Closes the dispute and carries the outcome to the order and
     * its payment.
     *
     * @throws BadRequestException when the dispute is already resolved
     */
    @Transactional
    public DisputeResponse resolveDispute(Long id, DisputeResolutionRequest request) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new BadRequestException("This dispute is already resolved");
        }

        Order order = orderRepository.findById(dispute.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", dispute.getOrderId()));

        dispute.setResolution(request.getResolution());
        dispute.setStatus(DisputeStatus.RESOLVED);
        disputeRepository.save(dispute);

        if (request.getOutcome() == DisputeOutcome.BUYER) {
            order.setStatus(OrderStatus.CANCELLED);
            if (order.getPaymentStatus().isRefundable()) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                setPaymentStatusForOrder(order.getId(), PaymentStatus.REFUNDED);
            }
            orderEventRepository.save(OrderEvent.builder()
                    .orderId(order.getId()).status(OrderStatus.CANCELLED).build());
        } else {
            order.setStatus(OrderStatus.COMPLETED);
            if (order.getPaymentStatus() == PaymentStatus.PAID
                    || order.getPaymentStatus() == PaymentStatus.HELD) {
                order.setPaymentStatus(PaymentStatus.SETTLED);
                setPaymentStatusForOrder(order.getId(), PaymentStatus.SETTLED);
            }
            orderEventRepository.save(OrderEvent.builder()
                    .orderId(order.getId()).status(OrderStatus.COMPLETED).build());
        }
        orderRepository.save(order);
        return mapToResponse(dispute);
    }

    /** The order moves to disputed and its money to escrow. */
    private void holdOrder(Order order) {
        order.setStatus(OrderStatus.DISPUTED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.HELD);
            setPaymentStatusForOrder(order.getId(), PaymentStatus.HELD);
        }
        orderRepository.save(order);
        orderEventRepository.save(OrderEvent.builder()
                .orderId(order.getId()).status(OrderStatus.DISPUTED).build());
    }

    /** Moves every payment row of the order to the given status. */
    private void setPaymentStatusForOrder(Long orderId, PaymentStatus status) {
        paymentRepository.findByOrderId(orderId).forEach(payment -> {
            payment.setStatus(status);
            paymentRepository.save(payment);
        });
    }

    private DisputeResponse mapToResponse(Dispute dispute) {
        return DisputeResponse.builder()
                .id(dispute.getId())
                .orderId(dispute.getOrderId())
                .raisedBy(dispute.getRaisedBy())
                .reason(dispute.getReason())
                .description(dispute.getDescription())
                .status(dispute.getStatus())
                .resolution(dispute.getResolution())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }
}
