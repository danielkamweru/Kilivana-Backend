package com.kilivana.backend.finance.service;

import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment;
import com.kilivana.backend.logistics.fulfilment.enums.FulfilmentStatus;
import com.kilivana.backend.logistics.fulfilment.repository.SellerFulfilmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for integrating seller fulfilment lifecycle with wallet balance management.
 * Releases pending earnings to available when fulfilment reaches delivery confirmation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfilmentWalletService {

    private final SellerFulfilmentRepository fulfilmentRepository;
    private final SellerWalletService walletService;
    private final FinancialAuditLogService auditLogService;

    /**
     * Called when a seller fulfilment status changes to COLLECTED (or DELIVERED).
     * Releases the seller's pending earnings for this fulfilment to available balance.
     *
     * @param fulfilmentId the fulfilment that was delivered/collected
     * @param actorId the user who triggered the status change (farmer, driver, admin)
     */
    @Transactional
    public void onFulfilmentDelivered(Long fulfilmentId, Long actorId) {
        SellerFulfilment fulfilment = fulfilmentRepository.findByIdForUpdate(fulfilmentId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerFulfilment", fulfilmentId));

        // Only release if fulfilment is in a terminal delivered state
        if (fulfilment.getStatus() != FulfilmentStatus.COLLECTED) {
            log.info("Fulfilment {} not in COLLECTED status ({}), skipping wallet release", fulfilmentId, fulfilment.getStatus());
            return;
        }

        // Check if already released
        if (walletService.hasReleaseTransaction(fulfilmentId)) {
            log.info("Fulfilment {} already released to available", fulfilmentId);
            return;
        }

        // The amount to release is the seller's net earnings for this fulfilment
        // We need to find the SALE_CREDIT transaction for this fulfilment
        BigDecimal amountToRelease = walletService.getPendingAmountForFulfilment(fulfilmentId);
        if (amountToRelease == null || amountToRelease.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("No pending amount to release for fulfilment {}", fulfilmentId);
            return;
        }

        walletService.releasePendingToAvailable(
                fulfilment.getSellerId(),
                fulfilmentId,
                amountToRelease,
                actorId
        );

        auditLogService.log(
                actorId,
                "FULFILMENT_RELEASE_EARNINGS",
                "SellerFulfilment",
                fulfilmentId,
                String.format("seller_id=%d, amount=%s, status=%s",
                        fulfilment.getSellerId(), amountToRelease, fulfilment.getStatus())
        );
    }

    /**
     * Called when a fulfilment is cancelled.
     * Reverses any pending credit for this fulfilment.
     */
    @Transactional
    public void onFulfilmentCancelled(Long fulfilmentId, Long actorId, String reason) {
        SellerFulfilment fulfilment = fulfilmentRepository.findByIdForUpdate(fulfilmentId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerFulfilment", fulfilmentId));

        if (fulfilment.getStatus() != FulfilmentStatus.CANCELLED) {
            return; // Only process actual cancellations
        }

        // Check if there's a pending sale credit to reverse
        BigDecimal pendingAmount = walletService.getPendingAmountForFulfilment(fulfilmentId);
        if (pendingAmount != null && pendingAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Reverse the pending credit by creating a negative adjustment
            walletService.reverseSaleCredit(fulfilment.getSellerId(), fulfilmentId, pendingAmount, actorId, reason);
        }

        auditLogService.log(
                actorId,
                "FULFILMENT_CANCELLED_REVERSAL",
                "SellerFulfilment",
                fulfilmentId,
                String.format("seller_id=%d, amount=%s, reason=%s",
                        fulfilment.getSellerId(), pendingAmount, reason)
        );
    }

    /**
     * Releases earnings for all fulfilments in an order that have been collected.
     * Useful for batch processing or admin actions.
     */
    @Transactional
    public void releaseCollectedFulfilmentsForOrder(Long orderId, Long actorId) {
        List<SellerFulfilment> fulfilments = fulfilmentRepository.findByOrderId(orderId);
        for (SellerFulfilment f : fulfilments) {
            if (f.getStatus() == FulfilmentStatus.COLLECTED) {
                try {
                    onFulfilmentDelivered(f.getId(), actorId);
                } catch (Exception e) {
                    log.error("Failed to release earnings for fulfilment {}: {}", f.getId(), e.getMessage());
                }
            }
        }
    }
}