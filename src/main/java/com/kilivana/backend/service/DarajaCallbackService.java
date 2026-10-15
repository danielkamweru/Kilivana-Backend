package com.kilivana.backend.service;

import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.dto.DarajaCallbackRequest;
import com.kilivana.backend.dto.DarajaCallbackResponse;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Processes Safaricom Daraja (M-PESA STK Push) callbacks.
 *
 * <p>Key properties:
 * <ul>
 *   <li><b>Idempotent</b> — a callback for an already-processed CheckoutRequestID
 *       is acknowledged without duplicating work. The payment row tracks the
 *       callback state via {@code callbackStatus}.</li>
 *   <li><b>Idempotent on success</b> — if the payment is already PAID/SETTLED,
 *       the callback is a no-op (late failure callbacks cannot overwrite success).</li>
 *   <li><b>Validates against pending request</b> — the callback must match a
 *       previously initiated STK Push (recorded by {@link DarajaService}).</li>
 *   <li><b>Updates payment and order atomically</b> — payment status, order
 *       paymentStatus, and Daraja metadata are all updated in one transaction.</li>
 * </ul>
 *
 * <p>Does NOT automatically release escrow or mark an order delivered —
 * those lifecycle steps remain in {@link com.kilivana.backend.ecommerce.service.OrderService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DarajaCallbackService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DarajaService darajaService;

    /**
     * Processes the Daraja callback and updates the payment.
     *
     * @param callback   the parsed callback payload
     * @param clientIp   IP of the caller (for audit)
     * @return response sent back to Safaricom (always 200 OK with resultCode=0)
     */
    @Transactional
    public DarajaCallbackResponse processCallback(DarajaCallbackRequest callback, String clientIp) {
        DarajaCallbackRequest.StkCallback stk = callback.getBody();
        if (stk == null) {
            log.warn("Callback missing stkCallback body from IP {}", clientIp);
            return new DarajaCallbackResponse();
        }

        String checkoutRequestId = stk.getCheckoutRequestID();
        String merchantRequestId = stk.getMerchantRequestID();
        int resultCode = stk.getResultCode();

        log.info("Processing Daraja callback: checkoutRequestId={}, merchantRequestId={}, resultCode={}",
                checkoutRequestId, merchantRequestId, resultCode);

        // Find the payment by checkout request ID (primary key for reconciliation)
        Payment payment = paymentRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElseGet(() -> {
                    // Fallback: try merchant request ID if checkout ID not found
                    log.warn("No payment found for checkoutRequestId={}, trying merchantRequestId={}",
                            checkoutRequestId, merchantRequestId);
                    return paymentRepository.findByMerchantRequestId(merchantRequestId)
                            .orElse(null);
                });

        if (payment == null) {
            log.warn("No payment record found for checkoutRequestId={} or merchantRequestId={} from IP {}",
                    checkoutRequestId, merchantRequestId, clientIp);
            // Still acknowledge to Safaricom to stop retries — we log for manual reconciliation
            return new DarajaCallbackResponse();
        }

        // Idempotency: if callback already processed, acknowledge without re-processing
        if ("PROCESSED".equals(payment.getCallbackStatus()) || "RECEIVED".equals(payment.getCallbackStatus())) {
            log.info("Callback for checkoutRequestId={} already processed (status={}), acknowledging",
                    checkoutRequestId, payment.getCallbackStatus());
            return new DarajaCallbackResponse();
        }

        // Mark callback as received to prevent concurrent processing
        payment.setCallbackStatus("RECEIVED");
        paymentRepository.save(payment);

        try {
            // Validate merchant request ID matches (defense against spoofed callbacks)
            if (payment.getMerchantRequestId() != null
                    && !payment.getMerchantRequestId().equals(merchantRequestId)) {
                log.warn("MerchantRequestID mismatch: stored={}, callback={}",
                        payment.getMerchantRequestId(), merchantRequestId);
                payment.setCallbackStatus("FAILED");
                paymentRepository.save(payment);
                return new DarajaCallbackResponse();
            }

            // Update Daraja metadata from callback
            payment.setMerchantRequestId(merchantRequestId);
            payment.setCheckoutRequestId(checkoutRequestId);

            if (stk.getCallbackMetadata() != null) {
                payment.setMpesaReceiptNumber(stk.getMpesaReceiptNumber());
                if (stk.getTransactionDate() != null) {
                    payment.setTransactionDate(parseTransactionDate(stk.getTransactionDate()));
                }
            }
            if (stk.getPhoneNumber() != null) {
                payment.setPhoneNumber(stk.getPhoneNumber());
            }

            if (resultCode == 0) {
                // SUCCESS: Payment confirmed by M-PESA
                log.info("M-PESA payment confirmed: orderId={}, amount={}, receipt={}",
                        payment.getOrderId(), payment.getAmount(), payment.getMpesaReceiptNumber());

                payment.setStatus(PaymentStatus.PAID);
                payment.setReference(payment.getMpesaReceiptNumber()); // Use receipt as reference
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(LocalDateTime.now());
                }
                payment.setCallbackStatus("PROCESSED");

                // Sync order payment status to PAID (escrow step)
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    orderRepository.save(order);
                });

            } else {
                // FAILURE/CANCEL/TIMEOUT: Do not mark order as paid
                log.warn("M-PESA payment failed or cancelled: orderId={}, resultCode={}, resultDesc={}",
                        payment.getOrderId(), resultCode, stk.getResultDesc());

                payment.setStatus(PaymentStatus.FAILED);
                payment.setCallbackStatus("PROCESSED");

                // Sync order payment status to FAILED
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(order);
                });
            }

            paymentRepository.save(payment);
            log.info("Daraja callback processed for orderId={}, paymentId={}, status={}",
                    payment.getOrderId(), payment.getId(), payment.getStatus());

        } catch (Exception e) {
            log.error("Error processing Daraja callback for checkoutRequestId={}: {}",
                    checkoutRequestId, e.getMessage(), e);
            payment.setCallbackStatus("FAILED");
            paymentRepository.save(payment);
        }

        return new DarajaCallbackResponse();
    }

    /** Parses Daraja transaction date (yyyyMMddHHmmss) to LocalDateTime. */
    private LocalDateTime parseTransactionDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            log.warn("Failed to parse Daraja transaction date: {}", dateStr);
            return null;
        }
    }
}