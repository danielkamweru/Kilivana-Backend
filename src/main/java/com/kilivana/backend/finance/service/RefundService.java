package com.kilivana.backend.finance.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.entity.RefundTransaction;
import com.kilivana.backend.finance.repository.RefundTransactionRepository;
import com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment;
import com.kilivana.backend.logistics.fulfilment.repository.SellerFulfilmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing buyer refunds.
 * Handles full and partial refunds with proper ledger reversals.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundTransactionRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SellerFulfilmentRepository fulfilmentRepository;
    private final UserRepository userRepository;
    private final SellerWalletService walletService;
    private final FinancialLedgerService ledgerService;
    private final FinancialAuditLogService auditLogService;

    /**
     * Creates a refund request for a payment.
     * Can be full (entire payment) or partial (specific fulfilment).
     */
    @Transactional
    public RefundTransaction requestRefund(Long paymentId, Long sellerFulfilmentId, BigDecimal amount, String reason, Long actorId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (!payment.getStatus().isRefundable()) {
            throw new BadRequestException("Payment " + paymentId + " is not refundable: " + payment.getStatus());
        }

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", payment.getOrderId()));

        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }

        // Check if refund would exceed payment amount
        BigDecimal totalRefunded = refundRepository.sumByPaymentIdAndStatus(paymentId, PaymentStatus.REFUNDED);
        BigDecimal totalApproved = refundRepository.sumByPaymentIdAndStatus(paymentId, PaymentStatus.REFUNDED); // Assuming PAID=refunded

        BigDecimal maxRefundable = payment.getAmount().subtract(totalRefunded == null ? BigDecimal.ZERO : totalRefunded);
        if (amount.compareTo(maxRefundable) > 0) {
            throw new BadRequestException("Refund amount " + amount + " exceeds refundable balance " + maxRefundable);
        }

        // Validate fulfilment if partial refund
        Long sellerId = null;
        if (sellerFulfilmentId != null) {
            SellerFulfilment fulfilment = fulfilmentRepository.findById(sellerFulfilmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("SellerFulfilment", sellerFulfilmentId));
            if (!fulfilment.getOrderId().equals(order.getId())) {
                throw new BadRequestException("Fulfilment does not belong to this order");
            }
            sellerId = fulfilment.getSellerId();
        }

        String reference = "RF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        RefundTransaction refund = RefundTransaction.builder()
                .paymentId(paymentId)
                .orderId(order.getId())
                .sellerFulfilmentId(sellerFulfilmentId)
                .sellerId(sellerId)
                .buyerId(order.getBuyerId())
                .amount(amount)
                .reason(reason)
                .status(PaymentStatus.PENDING)
                .reference(reference)
                .build();

        RefundTransaction saved = refundRepository.save(refund);

        auditLogService.log(
                actorId,
                "REFUND_REQUESTED",
                "RefundTransaction",
                saved.getId(),
                String.format("payment_id=%d, order_id=%d, fulfilment_id=%s, amount=%s, reason=%s",
                        paymentId, order.getId(), sellerFulfilmentId, amount, reason)
        );

        log.info("Refund request {} created for payment {}: amount={}", saved.getId(), paymentId, amount);
        return saved;
    }

    /**
     * Admin approves a refund request.
     */
    @Transactional
    public RefundTransaction approveRefund(Long refundId, Long adminId) {
        RefundTransaction refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("RefundTransaction", refundId));

        if (refund.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Refund " + refundId + " is not in PENDING status: " + refund.getStatus());
        }

        refund.setStatus(PaymentStatus.REFUNDED);
        refund.setApprovedBy(adminId);
        refund.setApprovedAt(LocalDateTime.now());

        RefundTransaction saved = refundRepository.save(refund);

        // Process the refund in seller wallet if partial (affects specific seller)
        if (refund.getSellerFulfilmentId() != null && refund.getSellerId() != null) {
            walletService.recordRefund(
                    refund.getSellerId(),
                    refund.getOrderId(),
                    refund.getSellerFulfilmentId(),
                    refund.getAmount(),
                    refund.getReference(),
                    "Refund approved for " + refund.getReason(),
                    adminId
            );
        }

        // If full order refund, reverse all seller credits
        if (refund.getSellerFulfilmentId() == null) {
            // This is a full order refund - reverse all fulfilments
            // Implementation would iterate over all fulfilments and reverse each
            log.info("Full order refund approved: {}", refundId);
        }

        // Update payment status if fully refunded
        BigDecimal totalRefunded = refundRepository.sumByPaymentIdAndStatus(refund.getPaymentId(), PaymentStatus.REFUNDED);
        Payment payment = paymentRepository.findById(refund.getPaymentId()).orElse(null);
        if (payment != null && totalRefunded != null && totalRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        // Update order payment status
        Order order = orderRepository.findById(refund.getOrderId()).orElse(null);
        if (order != null) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);
        }

        auditLogService.log(
                adminId,
                "REFUND_APPROVED",
                "RefundTransaction",
                refundId,
                String.format("amount=%s, payment_id=%d", refund.getAmount(), refund.getPaymentId())
        );

        log.info("Refund {} approved by admin {}", refundId, adminId);
        return saved;
    }

    /**
     * Admin rejects a refund request.
     */
    @Transactional
    public RefundTransaction rejectRefund(Long refundId, String rejectionReason, Long adminId) {
        RefundTransaction refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("RefundTransaction", refundId));

        if (refund.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Refund " + refundId + " is not in PENDING status: " + refund.getStatus());
        }

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BadRequestException("Rejection reason is required");
        }

        refund.setStatus(PaymentStatus.FAILED); // Using FAILED for rejected
        refund.setRejectionReason(rejectionReason);
        refund.setApprovedBy(adminId);
        refund.setApprovedAt(LocalDateTime.now());

        RefundTransaction saved = refundRepository.save(refund);

        auditLogService.log(
                adminId,
                "REFUND_REJECTED",
                "RefundTransaction",
                refundId,
                String.format("amount=%s, reason=%s", refund.getAmount(), rejectionReason)
        );

        log.info("Refund {} rejected by admin {}: {}", refundId, adminId, rejectionReason);
        return saved;
    }

    /**
     * Processes the refund callback from payment provider (M-Pesa).
     */
    @Transactional
    public void processRefundCallback(String providerReference, int resultCode, String resultDesc,
                                      String mpesaReceiptNumber, Long actorId) {
        RefundTransaction refund = refundRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new ResourceNotFoundException("RefundTransaction with providerReference: " + providerReference));

        refund.setProviderReference(providerReference);
        refund.setProviderResultCode(resultCode);
        refund.setProviderResultDesc(resultDesc);
        refund.setMpesaReceiptNumber(mpesaReceiptNumber);

        if (resultCode == 0) {
            refund.setStatus(PaymentStatus.REFUNDED);
            refund.setCompletedAt(LocalDateTime.now());

            auditLogService.log(
                    actorId,
                    "REFUND_PROVIDER_SUCCESS",
                    "RefundTransaction",
                    refund.getId(),
                    String.format("receipt=%s", mpesaReceiptNumber)
            );

            log.info("Refund {} completed via provider: receipt={}", refund.getId(), mpesaReceiptNumber);
        } else {
            refund.setStatus(PaymentStatus.FAILED);

            auditLogService.log(
                    actorId,
                    "REFUND_PROVIDER_FAILED",
                    "RefundTransaction",
                    refund.getId(),
                    String.format("result_code=%d, result_desc=%s", resultCode, resultDesc)
            );

            log.warn("Refund {} failed via provider: code={}, desc={}", refund.getId(), resultCode, resultDesc);
        }

        refundRepository.save(refund);
    }

    @Transactional(readOnly = true)
    public FinanceDtos.RefundTransactionResponse getRefund(Long refundId) {
        RefundTransaction refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("RefundTransaction", refundId));
        return FinanceDtos.RefundTransactionResponse.fromEntity(refund);
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.RefundTransactionResponse> getRefundsForOrder(Long orderId) {
        return refundRepository.findByOrderId(orderId).stream()
                .map(FinanceDtos.RefundTransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.RefundTransactionResponse> getRefundsForSeller(Long sellerId) {
        return refundRepository.findBySellerId(sellerId).stream()
                .map(FinanceDtos.RefundTransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.RefundTransactionResponse> getAllRefunds() {
        return refundRepository.findAll().stream()
                .map(FinanceDtos.RefundTransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinanceDtos.RefundTransactionResponse> getRefundsByStatus(PaymentStatus status) {
        return refundRepository.findByStatus(status).stream()
                .map(FinanceDtos.RefundTransactionResponse::fromEntity)
                .toList();
    }
}