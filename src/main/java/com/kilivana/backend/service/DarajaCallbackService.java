package com.kilivana.backend.service;

import com.kilivana.backend.config.DarajaProperties;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.dto.DarajaCallbackRequest;
import com.kilivana.backend.dto.DarajaCallbackResponse;
import com.kilivana.backend.ecommerce.entity.CallbackInbox;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.CallbackInboxRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final OrderService orderService;
    private final DarajaService darajaService;
    private final CallbackInboxRepository callbackInboxRepository;
    private final DarajaProperties darajaProperties;

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
        Integer resultCodeObj = stk.getResultCode();
        if (resultCodeObj == null) {
            log.warn("Callback missing ResultCode for checkoutRequestId={}; acknowledging without changes", checkoutRequestId);
            return new DarajaCallbackResponse();
        }
        int resultCode = resultCodeObj;
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
            // Durable inbox: store the callback so it can be matched when the payment
            // record is persisted (e.g., callback arrived before request IDs were saved).
            storeInInbox(checkoutRequestId, merchantRequestId, resultCode, resultDesc, null);
            // Still acknowledge to Safaricom to stop retries — the inbox entry will be
            // processed when the payment record catches up.
            return new DarajaCallbackResponse();
        }

        // Idempotency: if callback already fully processed, acknowledge without re-processing.
        // A PAID payment must never be overwritten by a later FAILED/CANCELLED callback.
        if ("PROCESSED".equals(payment.getCallbackStatus())) {
            log.info("Callback for checkoutRequestId={} already processed (paymentStatus={}), acknowledging",
                    checkoutRequestId, payment.getStatus());
            return new DarajaCallbackResponse();
        }

        // Guard against concurrent processing: if another thread is already handling this
        // callback (RECEIVED), acknowledge without duplicating work.
        if ("RECEIVED".equals(payment.getCallbackStatus())) {
            log.info("Callback for checkoutRequestId={} already being processed (RECEIVED), acknowledging",
                    checkoutRequestId);
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

                if (payment.getOrderId() == null && payment.getQuoteId() != null) {
                    // Payment-first checkout: the order does not exist yet. Create it now
                    // from the checkout quote and link the payment. If payment had failed
                    // or been cancelled, this branch is never reached and no order is made.
                    orderService.createOrderFromCheckoutQuote(payment.getQuoteId(), payment.getId());
                } else {
                    // Sync order payment status to PAID (escrow step)
                    orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                        order.setPaymentStatus(PaymentStatus.PAID);
                        orderRepository.save(order);
                    });
                }

            } else if (isCustomerCancellation(resultCode, resultDesc)) {
                // CUSTOMER CANCELLED: The buyer cancelled the STK Push on their phone
                log.warn("M-PESA payment cancelled by customer: orderId={}, resultCode={}, resultDesc={}",
                        payment.getOrderId(), resultCode, resultDesc);

                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCallbackStatus("PROCESSED");

                // Only update order payment status if no successful payment already exists
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    if (!isSuccessfulOrderPaymentStatus(order.getPaymentStatus())) {
                        order.setPaymentStatus(PaymentStatus.CANCELLED);
                        orderRepository.save(order);
                    }
                });

            } else {
                // OTHER FAILURE: Insufficient funds, invalid phone, timeout, etc.
                log.warn("M-PESA payment failed: orderId={}, resultCode={}, resultDesc={}",
                        payment.getOrderId(), resultCode, resultDesc);

                payment.setStatus(PaymentStatus.FAILED);
                payment.setCallbackStatus("PROCESSED");

                // Only update order payment status if no successful payment already exists
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    if (!isSuccessfulOrderPaymentStatus(order.getPaymentStatus())) {
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        orderRepository.save(order);
                    }
                });
            }

            paymentRepository.save(payment);
            log.info("Daraja callback processed for orderId={}, paymentId={}, status={}",
                    payment.getOrderId(), payment.getId(), payment.getStatus());

        } catch (Exception e) {
            log.error("Error processing Daraja callback for checkoutRequestId={}: {}: {}",
                    checkoutRequestId, e.getClass().getName(), e.getMessage(), e);
            payment.setCallbackStatus("FAILED");
            paymentRepository.save(payment);
            // Re-throw so the @Transactional rolls back — do not silently swallow DB errors
            // that would leave the payment in an inconsistent state. Safaricom will retry.
            throw new RuntimeException("Callback processing failed for checkoutRequestId=" + checkoutRequestId, e);
        }

        return new DarajaCallbackResponse();
    }

    /**
     * Returns true when the order already has a successful payment that must not
     * be overwritten by a failure or cancellation callback.
     */
    private boolean isSuccessfulOrderPaymentStatus(PaymentStatus status) {
        return status == PaymentStatus.PAID || status == PaymentStatus.HELD
                || status == PaymentStatus.SETTLED;
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

        Object resultCodeRaw = queryResult.get("ResultCode");
        Integer resultCodeObj = null;
        if (resultCodeRaw != null) {
            try {
                resultCodeObj = Integer.parseInt(String.valueOf(resultCodeRaw).trim());
            } catch (NumberFormatException e) {
                log.warn("Unparseable ResultCode {} for checkoutRequestId={}", resultCodeRaw, checkoutRequestID);
                return;
            }
        }
        if (resultCodeObj == null) {
            log.warn("Missing ResultCode for checkoutRequestId={}; skipping reconciliation", checkoutRequestID);
            return;
        }
        int resultCode = resultCodeObj;
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

        // Extract optional metadata from query result (Safaricom may omit these)
        Object amountObj = queryResult.get("Amount");
        if (amountObj != null) {
            try {
                Long amount = (amountObj instanceof Number) ? ((Number) amountObj).longValue()
                        : new java.math.BigDecimal(String.valueOf(amountObj)).longValue();
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
            } catch (Exception e) {
                log.warn("Could not parse Amount from query result for {}: {}", checkoutRequestID, e.getMessage());
            }
        }
        Object receiptObj = queryResult.get("MpesaReceiptNumber");
        if (receiptObj != null) {
            payment.setMpesaReceiptNumber(String.valueOf(receiptObj));
        }
        Object txDateObj = queryResult.get("TransactionDate");
        if (txDateObj != null) {
            String txDate = String.valueOf(txDateObj);
            if (payment.getTransactionDate() == null) {
                payment.setTransactionDate(parseTransactionDate(txDate));
            }
        }
        Object phoneObj = queryResult.get("PhoneNumber");
        if (phoneObj != null) {
            payment.setPhoneNumber(String.valueOf(phoneObj));
        }

        if (resultCode == 0) {
            log.info("M-PESA payment confirmed via reconciliation: orderId={}, amount={}, receipt={}",
                    payment.getOrderId(), payment.getAmount(), payment.getMpesaReceiptNumber());

            payment.setStatus(PaymentStatus.PAID);
            if (payment.getMpesaReceiptNumber() != null) {
                payment.setReference(payment.getMpesaReceiptNumber());
            } else if (payment.getReference() == null) {
                payment.setReference("STK-" + checkoutRequestID);
            }
            if (payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
            payment.setCallbackStatus("PROCESSED");

            if (payment.getOrderId() == null && payment.getQuoteId() != null) {
                orderService.createOrderFromCheckoutQuote(payment.getQuoteId(), payment.getId());
            } else {
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    orderRepository.save(order);
                });
            }
        } else if (isCustomerCancellation(resultCode, resultDesc)) {
            log.warn("M-PESA payment cancelled by customer via reconciliation: orderId={}, resultCode={}, resultDesc={}",
                    payment.getOrderId(), resultCode, resultDesc);

            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCallbackStatus("PROCESSED");

            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                if (!isSuccessfulOrderPaymentStatus(order.getPaymentStatus())) {
                    order.setPaymentStatus(PaymentStatus.CANCELLED);
                    orderRepository.save(order);
                }
            });
        } else {
            log.warn("M-PESA payment failed via reconciliation: orderId={}, resultCode={}, resultDesc={}",
                    payment.getOrderId(), resultCode, resultDesc);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setCallbackStatus("PROCESSED");

            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                if (!isSuccessfulOrderPaymentStatus(order.getPaymentStatus())) {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(order);
                }
            });
        }

        paymentRepository.save(payment);
        log.info("Reconciliation complete for orderId={}, paymentId={}, status={}",
                payment.getOrderId(), payment.getId(), payment.getStatus());
    }

    /**
     * Marks a PENDING STK Push payment as FAILED when it has exceeded the local
     * timeout deadline without a callback and Safaricom cannot be reconciled.
     * For pre-order payments (no order yet) this guarantees no order is ever
     * created; for payments linked to an order, the order's payment status is
     * synced to FAILED unless it is already successful.
     *
     * @param paymentId the id of the pending payment to fail
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void timeoutPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            // Already resolved by an intervening callback/reconciliation; nothing to do.
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setCallbackStatus("PROCESSED");
        payment.setDarajaResultDesc("STK Push timed out: no callback received before deadline");
        paymentRepository.save(payment);

        if (payment.getOrderId() != null) {
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                if (!isSuccessfulOrderPaymentStatus(order.getPaymentStatus())) {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(order);
                }
            });
        }

        log.info("Timed out payment {} (orderId={}) marked FAILED", payment.getId(), payment.getOrderId());
    }

    /**
     * Durably stores a callback in the inbox when no payment record can be matched.
     * The entry is processed later when the payment record is persisted with matching
     * CheckoutRequestID and MerchantRequestID.
     */
    private void storeInInbox(String checkoutRequestId, String merchantRequestId,
                              int resultCode, String resultDesc, String rawPayload) {
        if (checkoutRequestId == null) {
            log.warn("Cannot store callback in inbox: null checkoutRequestId");
            return;
        }
        CallbackInbox entry = CallbackInbox.builder()
                .checkoutRequestId(checkoutRequestId)
                .merchantRequestId(merchantRequestId != null ? merchantRequestId : "")
                .resultCode(resultCode)
                .resultDesc(resultDesc)
                .rawPayload(rawPayload)
                .processed(false)
                .build();
        try {
            callbackInboxRepository.save(entry);
            log.info("Stored unmatched callback in inbox: checkoutRequestId={}, resultCode={}",
                    checkoutRequestId, resultCode);
        } catch (Exception e) {
            // Unique constraint violation means a duplicate inbox entry — safe to ignore.
            log.warn("Failed to store callback in inbox (may be duplicate): checkoutRequestId={}",
                    checkoutRequestId);
        }
    }
}
