package com.kilivana.backend.service;

import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Scheduled reconciliation for M-PESA STK Push payments that remain PENDING.
 *
 * <p>Safaricom callbacks can be lost, delayed, or fail to reach the backend.
 * This job polls Safaricom's STK Push Query API for payments that have been
 * PENDING long enough that their callback should have arrived, and updates
 * their status based on the confirmed transaction result.
 *
 * <p>Runs every 10 minutes by default. Only payments older than 10 minutes
 * are reconciled, to avoid racing with an in-flight callback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private static final long RECONCILIATION_MIN_AGE_MINUTES = 2;

    private final PaymentRepository paymentRepository;
    private final DarajaService darajaService;
    private final DarajaCallbackService callbackService;

    /**
     * Reconciles all PENDING payments that are older than the minimum age and
     * have a Daraja CheckoutRequestID. Queries Safaricom for each one and
     * updates the payment status based on the confirmed result.
     */
    @Scheduled(fixedDelay = 120_000, initialDelay = 120_000)
    @Transactional
    public void reconcilePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(RECONCILIATION_MIN_AGE_MINUTES);
        List<Payment> pending = paymentRepository.findByStatus(PaymentStatus.PENDING)
                .stream()
                .filter(p -> p.getCheckoutRequestId() != null
                        && p.getCreatedAt() != null
                        && p.getCreatedAt().isBefore(cutoff))
                .toList();

        if (pending.isEmpty()) {
            return;
        }

        log.info("Reconciling {} PENDING payment(s) older than {} minutes",
                pending.size(), RECONCILIATION_MIN_AGE_MINUTES);

        for (Payment payment : pending) {
            String checkoutRequestId = payment.getCheckoutRequestId();
            if (checkoutRequestId == null) {
                continue;
            }

            try {
                Map<String, Object> queryResult = darajaService.queryStkPush(checkoutRequestId);
                if (queryResult != null) {
                    callbackService.reconcileFromQuery(checkoutRequestId, queryResult);
                } else {
                    log.warn("Could not query STK Push status for CheckoutRequestID={}; will retry on next cycle",
                            checkoutRequestId);
                }
            } catch (Exception e) {
                log.error("Error reconciling CheckoutRequestID={}: {}", checkoutRequestId, e.getMessage(), e);
            }
        }
    }
}
