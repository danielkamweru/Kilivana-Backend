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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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
 *   <li><b>Distinguishes cancellation from failure</b> — a customer-cancelled
 *       STK Push becomes {@code CANCELLED}; other non-zero results become
 *       {@code FAILED}.</li>
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
        String resultDesc = stk.getResultDesc();

        log.info("Processing Daraja callback: checkoutRequestId={}, merchantRequestId={}, resultCode={}, resultDesc={}",
                checkoutRequestId, merchantRequestId, resultCode, resultDesc);

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

        // Idempotency: if callback already processed, acknowledge without re-processing.
        // A PAID payment must never be overwritten by a later FAILED/CANCELLED callback.
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
                payment.setDarajaResultCode(resultCode);
                payment.setDarajaResultDesc(resultDesc);
                paymentRepository.save(payment);
                return new DarajaCallbackResponse();
            }

            // Update Daraja metadata from callback
            payment.setMerchantRequestId(merchantRequestId);
            payment.setCheckoutRequestId(checkoutRequestId);
            payment.setDarajaResultCode(resultCode);
            payment.setDarajaResultDesc(resultDesc);

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
                Long callbackAmount = stk.getAmount();
                if (callbackAmount != null) {
                    BigDecimal callbackAmountDecimal = BigDecimal.valueOf(callbackAmount);
                    if (callbackAmountDecimal.compareTo(payment.getAmount()) != 0) {
                        log.warn("M-PESA amount mismatch: expected={}, callback={}, orderId={}",
                                payment.getAmount(), callbackAmountDecimal, payment.getOrderId());
                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setCallbackStatus("PROCESSED");
                        payment.setDarajaResultCode(resultCode);
                        payment.setDarajaResultDesc("Amount mismatch: expected " + payment.getAmount()
                                + ", received " + callbackAmountDecimal);
                        paymentRepository.save(payment);
                        orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                            order.setPaymentStatus(PaymentStatus.FAILED);
                            orderRepository.save(order);
                        });
                        return new DarajaCallbackResponse();
                    }
                }

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

            } else if (isCustomerCancellation(resultCode, resultDesc)) {
                // CUSTOMER CANCELLED: The buyer cancelled the STK Push on their phone
                log.warn("M-PESA payment cancelled by customer: orderId={}, resultCode={}, resultDesc={}",
                        payment.getOrderId(), resultCode, resultDesc);

                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCallbackStatus("PROCESSED");

                // Sync order payment status to CANCELLED
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.CANCELLED);
                    orderRepository.save(order);
                });

            } else {
                // OTHER FAILURE: Insufficient funds, invalid phone, timeout, etc.
                log.warn("M-PESA payment failed: orderId={}, resultCode={}, resultDesc={}",
                        payment.getOrderId(), resultCode, resultDesc);

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

    /**
     * Determines whether a Daraja result indicates the customer cancelled
     * the STK Push on their phone, as opposed to a system failure.
     *
     * <p>Safaricom returns various result codes for cancellations depending
     * on the environment. The result description typically contains
     * cancellation language (e.g. "cancelled by user", "user cancelled").
     */
    private boolean isCustomerCancellation(int resultCode, String resultDesc) {
        if (resultDesc != null && resultDesc.toLowerCase(java.util.Locale.ROOT).contains("cancel")) {
            return true;
        }
        // Safaricom sandbox commonly uses 1032 for customer cancellation
        return resultCode == 1032;
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

    /**
     * Reconciles a payment using a Safaricom STK Push Query result.
     * Used when the callback was lost or delayed. Idempotent: already-processed
     * payments are skipped.
     *
     * @param checkoutRequestID the Daraja CheckoutRequestID to reconcile
     * @param queryResult       the raw query response map from Daraja
     */
    @Transactional
    public void reconcileFromQuery(String checkoutRequestID, Map<String, Object> queryResult) {
        if (queryResult == null) {
            log.warn("Cannot reconcile CheckoutRequestID={}: null query result", checkoutRequestID);
            return;
        }

        Integer resultCodeObj = (Integer) queryResult.get("ResultCode");
        int resultCode = resultCodeObj == null ? -1 : resultCodeObj;
        String merchantRequestId = (String) queryResult.get("MerchantRequestID");
        String resultDesc = (String) queryResult.get("ResultDesc");

        log.info("Reconciling CheckoutRequestID={} from query: resultCode={}, merchantRequestId={}",
                checkoutRequestID, resultCode, merchantRequestId);

        Payment payment = paymentRepository.findByCheckoutRequestId(checkoutRequestID)
                .orElse(null);
        if (payment == null) {
            log.warn("No payment record found for CheckoutRequestID={}", checkoutRequestID);
            return;
        }

        // Idempotency: skip if already processed. A PAID payment must not be
        // overwritten by a later FAILED/CANCELLED reconciliation.
        if ("PROCESSED".equals(payment.getCallbackStatus()) || "RECEIVED".equals(payment.getCallbackStatus())) {
            log.info("CheckoutRequestID={} already processed (status={}), skipping reconciliation",
                    checkoutRequestID, payment.getCallbackStatus());
            return;
        }

        payment.setCallbackStatus("RECEIVED");
        payment.setMerchantRequestId(merchantRequestId);
        payment.setCheckoutRequestId(checkoutRequestID);
        payment.setDarajaResultCode(resultCode);
        payment.setDarajaResultDesc(resultDesc);

        // Extract metadata from query result (Safaricom returns top-level fields on success)
        Object amountObj = queryResult.get("Amount");
        if (amountObj != null) {
            Long amount = ((Number) amountObj).longValue();
            payment.setMpesaReceiptNumber((String) queryResult.get("MpesaReceiptNumber"));
            String txDate = (String) queryResult.get("TransactionDate");
            if (txDate != null) {
                payment.setTransactionDate(parseTransactionDate(txDate));
            }
            payment.setPhoneNumber((String) queryResult.get("PhoneNumber"));

            if (resultCode == 0) {
                BigDecimal callbackAmountDecimal = BigDecimal.valueOf(amount);
                if (callbackAmountDecimal.compareTo(payment.getAmount()) != 0) {
                    log.warn("M-PESA amount mismatch (reconciliation): expected={}, query={}, orderId={}",
                            payment.getAmount(), callbackAmountDecimal, payment.getOrderId());
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setCallbackStatus("PROCESSED");
                    payment.setDarajaResultDesc("Amount mismatch: expected " + payment.getAmount()
                            + ", received " + callbackAmountDecimal);
                    paymentRepository.save(payment);
                    orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        orderRepository.save(order);
                    });
                    return;
                }

                log.info("M-PESA payment confirmed via reconciliation: orderId={}, amount={}, receipt={}",
                        payment.getOrderId(), payment.getAmount(), payment.getMpesaReceiptNumber());

                payment.setStatus(PaymentStatus.PAID);
                payment.setReference(payment.getMpesaReceiptNumber());
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(LocalDateTime.now());
                }
                payment.setCallbackStatus("PROCESSED");

                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    orderRepository.save(order);
                });
            } else if (isCustomerCancellation(resultCode, resultDesc)) {
                log.warn("M-PESA payment cancelled by customer via reconciliation: orderId={}, resultCode={}, resultDesc={}",
                        payment.getOrderId(), resultCode, resultDesc);

                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCallbackStatus("PROCESSED");

                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.CANCELLED);
                    orderRepository.save(order);
                });
            } else {
                log.warn("M-PESA payment failed via reconciliation: orderId={}, resultCode={}, resultDesc={}",
                        payment.getOrderId(), resultCode, resultDesc);

                payment.setStatus(PaymentStatus.FAILED);
                payment.setCallbackStatus("PROCESSED");

                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(order);
                });
            }
        } else {
            // No Amount in result — treat as failure
            log.warn("STK Push Query result for CheckoutRequestID={} has no Amount field; treating as failed",
                    checkoutRequestID);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setCallbackStatus("PROCESSED");
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
            });
        }

        paymentRepository.save(payment);
        log.info("Reconciliation complete for orderId={}, paymentId={}, status={}",
                payment.getOrderId(), payment.getId(), payment.getStatus());
    }
}
