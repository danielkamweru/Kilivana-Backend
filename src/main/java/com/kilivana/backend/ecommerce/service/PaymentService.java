package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.dto.DarajaCallbackRequest;
import com.kilivana.backend.dto.StkPushInitiateRequest;
import com.kilivana.backend.dto.StkPushInitiateResponse;
import com.kilivana.backend.dto.StkPushResponse;
import com.kilivana.backend.ecommerce.dto.PaymentResponse;
import com.kilivana.backend.ecommerce.entity.CallbackInbox;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.CallbackInboxRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.service.DarajaCallbackService;
import com.kilivana.backend.service.DarajaService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@PreAuthorize("@resourceAuthorization.admin()")
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DarajaService darajaService;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Autowired(required = false)
    private CallbackInboxRepository callbackInboxRepository;

    @Autowired(required = false)
    private DarajaCallbackService darajaCallbackService;

    @Autowired
    private PaymentService self;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          DarajaService darajaService,
                          UserRepository userRepository,
                          OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.darajaService = darajaService;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    @PostConstruct
    void init() {
    }

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

    @PreAuthorize("@resourceAuthorization.sellerPayment(#userId, #userRole, #request.orderId)")
    public StkPushInitiateResponse initiateStkPush(Long userId, UserRole userRole,
                                                   StkPushInitiateRequest request) {
        Order order = loadOrder(request.getOrderId());

        List<OrderItem> items = order.getOrderItems() == null ? List.of() : order.getOrderItems();
        boolean authorized = items.stream()
                .anyMatch(item -> item.getSellerId().equals(userId));
        if (!authorized) {
            throw new BadRequestException("You are not authorized to request payment for this order");
        }

        return (self != null ? self : this).startStkPush(order, request);
    }

    @PreAuthorize("@resourceAuthorization.buyerPayment(#userId, #userRole, #request.orderId)")
    public StkPushInitiateResponse initiateStkPushForBuyer(Long userId, UserRole userRole,
                                                           StkPushInitiateRequest request) {
        Order order = loadOrder(request.getOrderId());

        if (order.getBuyerId() == null || !order.getBuyerId().equals(userId)) {
            throw new BadRequestException("You are not authorized to pay for this order");
        }

        return (self != null ? self : this).startStkPush(order, request);
    }

    @Transactional(readOnly = true)
    private Order loadOrder(Long orderId) {
        if (orderId == null) {
            throw new BadRequestException("Order ID is required");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StkPushInitiateResponse startStkPush(Order order, StkPushInitiateRequest request) {
        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot request payment for a " + order.getStatus().wire() + " order");
        }

        BigDecimal paidAmount = paymentRepository.findByOrderId(order.getId()).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.HELD
                        || p.getStatus() == PaymentStatus.SETTLED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = order.getTotal().subtract(paidAmount);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No outstanding balance on this order");
        }

        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
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

        if (!phoneNumber.matches("^254(7\\d{8}|1\\d{8}|11\\d{7})$")) {
            throw new BadRequestException("Phone number must be a valid Safaricom number in 2547XXXXXXXX, 2541XXXXXXXX, or 25411XXXXXXX format");
        }

        int amountKes = outstanding.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        if (amountKes <= 0) {
            throw new BadRequestException("Outstanding amount must be positive");
        }

        List<Payment> existingPending = paymentRepository.findByOrderId(order.getId()).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING
                        && p.getCheckoutRequestId() != null)
                .toList();
        if (!existingPending.isEmpty()) {
            throw new BadRequestException("A payment is already being processed for this order. "
                    + "Please wait for the current transaction to complete before retrying.");
        }

        String accountRef = "ORD-" + order.getCode().replace("ORD-", "");
        String transactionDesc = "Order " + order.getCode();

        Payment pending = (self != null ? self : this).createPendingPayment(order, phoneNumber, outstanding);

        StkPushResponse darajaResponse;
        try {
            darajaResponse = darajaService.initiateStkPush(
                    phoneNumber, amountKes, accountRef, transactionDesc);
        } catch (Exception e) {
            (self != null ? self : this).markPaymentFailed(pending, "Daraja initiation failed: " + e.getMessage());
            throw e;
        }

        Payment saved = (self != null ? self : this).updatePaymentWithDarajaDetails(
                pending, darajaResponse.getMerchantRequestID(), darajaResponse.getCheckoutRequestID());

        (self != null ? self : this).processInboxForPayment(saved, darajaResponse.getMerchantRequestID());

        StkPushInitiateResponse response = new StkPushInitiateResponse();
        response.setPaymentId(saved.getId());
        response.setOrderId(order.getId());
        response.setAmount(outstanding);
        response.setStatus(saved.getStatus().wire());
        response.setDarajaResponse(darajaResponse);
        response.setCreatedAt(saved.getCreatedAt());
        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createPendingPayment(Order order, String phoneNumber, BigDecimal amount) {
        Payment p = Payment.builder()
                .orderId(order.getId())
                .method(PaymentMethod.MPESA)
                .reference("STK-PENDING")
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .phoneNumber(phoneNumber)
                .callbackStatus("PENDING")
                .build();
        return paymentRepository.save(p);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentFailed(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setDarajaResultDesc(reason);
        paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment updatePaymentWithDarajaDetails(Payment payment,
                                                  String merchantRequestId,
                                                  String checkoutRequestId) {
        payment.setMerchantRequestId(merchantRequestId);
        payment.setCheckoutRequestId(checkoutRequestId);
        payment.setReference("STK-" + checkoutRequestId);
        payment.setCallbackStatus("PENDING");
        return paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processInboxForPayment(Payment payment, String merchantRequestId) {
        if (callbackInboxRepository == null || darajaCallbackService == null) {
            return;
        }
        String checkoutRequestId = payment.getCheckoutRequestId();
        if (checkoutRequestId == null) {
            return;
        }

        var inboxEntry = callbackInboxRepository
                .findByCheckoutRequestIdAndMerchantRequestIdAndProcessedFalse(
                        checkoutRequestId, merchantRequestId);
        if (inboxEntry.isEmpty()) {
            return;
        }

        CallbackInbox entry = inboxEntry.get();
        log.info("Processing early callback from inbox: checkoutRequestId={}, resultCode={}",
                checkoutRequestId, entry.getResultCode());

        DarajaCallbackRequest callbackRequest = buildCallbackRequestFromInbox(entry);
        darajaCallbackService.processCallback(callbackRequest, "callback-inbox");

        entry.setProcessed(true);
        callbackInboxRepository.save(entry);
    }

    private DarajaCallbackRequest buildCallbackRequestFromInbox(CallbackInbox entry) {
        DarajaCallbackRequest.StkCallback stk = new DarajaCallbackRequest.StkCallback();
        stk.setMerchantRequestID(entry.getMerchantRequestId());
        stk.setCheckoutRequestID(entry.getCheckoutRequestId());
        stk.setResultCode(entry.getResultCode());
        stk.setResultDesc(entry.getResultDesc());
        DarajaCallbackRequest request = new DarajaCallbackRequest();
        request.setBody(stk);
        return request;
    }

    @PreAuthorize("@resourceAuthorization.payment(#id)")
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    @PreAuthorize("@resourceAuthorization.order(#orderId)")
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @PreAuthorize("@resourceAuthorization.order(#orderId) || @resourceAuthorization.orderSeller(#orderId)")
    @Transactional(readOnly = true)
    public Payment getLatestPaymentByOrder(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Payment", orderId);
        }
        return payments.stream()
                .max(java.util.Comparator.comparing(Payment::getCreatedAt))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", orderId));
    }

    @PreAuthorize("@resourceAuthorization.seller(#sellerId) || @resourceAuthorization.admin()")
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsBySeller(Long sellerId) {
        List<Payment> all = paymentRepository.findAll();
        return all.stream()
                .filter(payment -> orderItemRepository.findByOrderId(payment.getOrderId()).stream()
                        .anyMatch(item -> item.getSellerId().equals(sellerId)))
                .toList();
    }

    @Transactional
    public Payment updatePaymentStatus(Long id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        payment.setStatus(status);
        if (status == PaymentStatus.PAID && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }
        Payment saved = paymentRepository.save(payment);
        orderRepository.findByIdForUpdate(payment.getOrderId()).ifPresent(order -> {
            order.setPaymentStatus(status);
            orderRepository.save(order);
            if (status == PaymentStatus.REFUNDED || status == PaymentStatus.FAILED) {
                orderService.reverseSalesForRefund(order);
            } else if (status == PaymentStatus.PAID) {
                orderService.recordSalesForPaidDelivery(order);
            }
        });
        return saved;
    }

    private static boolean isSuccessfulStatus(PaymentStatus status) {
        return status == PaymentStatus.PAID || status == PaymentStatus.HELD
                || status == PaymentStatus.SETTLED;
    }

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
                .merchantRequestId(payment.getMerchantRequestId())
                .checkoutRequestId(payment.getCheckoutRequestId())
                .phoneNumber(payment.getPhoneNumber())
                .mpesaReceiptNumber(payment.getMpesaReceiptNumber())
                .transactionDate(payment.getTransactionDate())
                .callbackStatus(payment.getCallbackStatus())
                .darajaResultCode(payment.getDarajaResultCode())
                .darajaResultDesc(payment.getDarajaResultDesc())
                .build();
    }
}
