package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.ecommerce.dto.PaymentResponse;
import com.kilivana.backend.dto.StkPushInitiateRequest;
import com.kilivana.backend.dto.StkPushInitiateResponse;
import com.kilivana.backend.dto.StkPushResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.service.DarajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Records payment attempts and mirrors their status onto the parent order.
 *
 * <p>The order's {@code paymentStatus} is a derived view of its payment rows — it is updated
 * here so the two never drift. A payment method is validated against the panel's enum so a
 * caller cannot store a provider string that nothing will ever read back.
 *
 * <p>Daraja STK Push initiation is handled here: the service calculates the outstanding
 * balance from the order, initiates the STK Push via {@link DarajaService}, and persists
 * the payment attempt with Daraja metadata for callback reconciliation.
 */
@Service
@PreAuthorize("@resourceAuthorization.admin()")
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DarajaService darajaService;
    private final UserRepository userRepository;

    /**
     * Creates a new payment record for an order. Validates the payment method
     * against the known panel enum (MPESA, BANK, CARD).
     *
     * @throws ResourceNotFoundException if the order does not exist
     * @throws BadRequestException if the payment method is unknown
     */
    @Transactional
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

    /**
     * Initiates an M-PESA STK Push for an order.
     *
     * <p>Calculates the outstanding balance from the order, validates that the
     * caller (farmer/supplier) owns the order, and initiates the STK Push to the
     * buyer's phone. The payment is recorded as PENDING with Daraja metadata for
     * callback reconciliation.
     *
     * @param userId    authenticated user ID (farmer or supplier)
     * @param userRole  authenticated user role (FARMER or SUPPLIER)
     * @param request   STK Push initiation request with order ID and optional phone
     * @return response containing payment ID and Daraja STK Push response
     * @throws ResourceNotFoundException if the order does not exist
     * @throws BadRequestException if user not authorized, order not payable, or phone invalid
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.sellerPayment(#userId, #userRole, #request.orderId)")
    public StkPushInitiateResponse initiateStkPush(Long userId, UserRole userRole,
                                                   StkPushInitiateRequest request) {
        Order order = loadOrder(request.getOrderId());

        // Authorization: only the farmer or supplier who owns items in this order may initiate
        List<OrderItem> items = order.getOrderItems() == null ? List.of() : order.getOrderItems();
        boolean authorized = items.stream()
                .anyMatch(item -> item.getSellerId().equals(userId));
        if (!authorized) {
            throw new BadRequestException("You are not authorized to request payment for this order");
        }

        return startStkPush(order, request);
    }

    /**
     * Initiates an M-PESA STK Push for an order at the buyer's request.
     *
     * <p>This is the checkout path: the buyer pays for their own order, so the caller
     * must be the buyer of that order. The amount is still the order's outstanding
     * balance, computed on the server: the client cannot choose what to pay.
     *
     * @param userId    authenticated user ID (the buyer)
     * @param userRole  authenticated user role (BUYER)
     * @param request   STK Push initiation request with order ID and optional phone
     * @return response containing payment ID and Daraja STK Push response
     * @throws ResourceNotFoundException if the order does not exist
     * @throws BadRequestException if the caller is not the buyer, the order is not
     *         payable, there is no outstanding balance, or the phone is invalid
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.buyerPayment(#userId, #userRole, #request.orderId)")
    public StkPushInitiateResponse initiateStkPushForBuyer(Long userId, UserRole userRole,
                                                           StkPushInitiateRequest request) {
        Order order = loadOrder(request.getOrderId());

        // Authorization: only the buyer of this order may pay for it. Checked here as
        // well as in the authorization rule, so the service holds the rule even when it
        // is reached without the secured proxy.
        if (order.getBuyerId() == null || !order.getBuyerId().equals(userId)) {
            throw new BadRequestException("You are not authorized to pay for this order");
        }

        return startStkPush(order, request);
    }

    /** Loads the order an STK Push is requested for. */
    private Order loadOrder(Long orderId) {
        if (orderId == null) {
            throw new BadRequestException("Order ID is required");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    /**
     * The shared STK Push path: the order must be payable, the amount is the order's
     * outstanding balance, the phone is the buyer's, and the attempt is recorded as
     * PENDING for the callback to reconcile.
     */
    private StkPushInitiateResponse startStkPush(Order order, StkPushInitiateRequest request) {
        // Order must be in a payable state
        if (order.getStatus() == com.kilivana.backend.common.enums.OrderStatus.CANCELLED
                || order.getStatus() == com.kilivana.backend.common.enums.OrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot request payment for a " + order.getStatus().wire() + " order");
        }

        // Calculate outstanding balance: total - sum of successful payments
        BigDecimal paidAmount = paymentRepository.findByOrderId(order.getId()).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.HELD
                        || p.getStatus() == PaymentStatus.SETTLED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = order.getTotal().subtract(paidAmount);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No outstanding balance on this order");
        }

        // Determine buyer's phone number
        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            // Fetch buyer phone from user profile
            Long buyerId = order.getBuyerId();
            if (buyerId == null) {
                throw new BadRequestException("Buyer not found on order");
            }
            Optional<User> buyerOptional = userRepository.findById(buyerId);
            if (!buyerOptional.isPresent()) {
                throw new BadRequestException("Buyer user not found");
            }
            phoneNumber = buyerOptional.get().getPhone();
            if (phoneNumber == null || phoneNumber.isBlank()) {
                throw new BadRequestException("Buyer phone number is not set");
            }
        }

        // Validate phone format (Kenyan Safaricom: 2547XXXXXXXX or 2541XXXXXXXX)
        if (!phoneNumber.matches("^254[17]\\d{8}$")) {
            throw new BadRequestException("Phone number must be in 2547XXXXXXXX or 2541XXXXXXXX format");
        }

        int amountKes = outstanding.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        if (amountKes <= 0) {
            throw new BadRequestException("Outstanding amount must be positive");
        }

        // Generate account reference and transaction description
        String accountRef = "ORD-" + order.getCode().replace("ORD-", "");
        String transactionDesc = "Order " + order.getCode();

        // Initiate STK Push
        StkPushResponse darajaResponse = darajaService.initiateStkPush(
                phoneNumber, amountKes, accountRef, transactionDesc);

        // Record payment attempt with Daraja metadata
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .method(PaymentMethod.MPESA)
                .reference("STK-" + darajaResponse.getCheckoutRequestID()) // provisional, updated on callback
                .amount(outstanding)
                .status(PaymentStatus.PENDING)
                .merchantRequestId(darajaResponse.getMerchantRequestID())
                .checkoutRequestId(darajaResponse.getCheckoutRequestID())
                .phoneNumber(phoneNumber)
                .callbackStatus("PENDING")
                .build();

        Payment saved = paymentRepository.save(payment);

        // Build response
        StkPushInitiateResponse response = new StkPushInitiateResponse();
        response.setPaymentId(saved.getId());
        response.setOrderId(order.getId());
        response.setAmount(outstanding);
        response.setStatus(saved.getStatus().wire());
        response.setDarajaResponse(darajaResponse);
        response.setCreatedAt(saved.getCreatedAt());
        return response;
    }

    /**
     * Retrieves a payment by its ID.
     *
     * @throws ResourceNotFoundException if the payment does not exist
     */
    @PreAuthorize("@resourceAuthorization.payment(#id)")
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    /** Returns all payments for a specific order. */
    @PreAuthorize("@resourceAuthorization.order(#orderId)")
    public List<Payment> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /** Returns all payments in the system. */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Updates a payment's status and syncs it to the parent order.
     * Sets {@code paidAt} when transitioning to PAID (records when money arrived).
     *
     * @throws ResourceNotFoundException if the payment does not exist
     */
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
        // the payment rather than being set separately and drifting. Only forward
        // moves are applied: an accepted STK Push (PENDING) must not flip the order
        // to paid, and a failure must not regress it either.
        orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
            PaymentStatus current = order.getPaymentStatus();
            if (current != null && status.ordinal() <= current.ordinal()) {
                return;
            }
            order.setPaymentStatus(status);
            orderRepository.save(order);
        });
        return saved;
    }

    /** Maps a payment entity to its panel-facing response DTO. */
    @PreAuthorize("@resourceAuthorization.payment(#payment.id)")
    public PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .method(payment.getMethod())
                .reference(payment.getReference())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}