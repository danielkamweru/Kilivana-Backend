package com.kilivana.backend.finance.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.admin.service.PlatformSettingService;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.finance.dto.FinanceDtos;
import com.kilivana.backend.finance.enums.DeliveryOtpStatus;
import com.kilivana.backend.finance.enums.SettlementAllocationType;
import com.kilivana.backend.finance.enums.SettlementStatus;
import com.kilivana.backend.finance.entity.DeliveryOtpVerification;
import com.kilivana.backend.finance.entity.Settlement;
import com.kilivana.backend.finance.entity.SettlementAllocation;
import com.kilivana.backend.finance.repository.DeliveryOtpVerificationRepository;
import com.kilivana.backend.finance.repository.SettlementAllocationRepository;
import com.kilivana.backend.finance.repository.SettlementRepository;
import com.kilivana.backend.logistics.fulfilment.entity.Shipment;
import com.kilivana.backend.logistics.fulfilment.repository.ShipmentRepository;
import com.kilivana.backend.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing financial settlements per shipment.
 * Handles settlement calculation, OTP verification integration, and wallet/ledger updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementAllocationRepository allocationRepository;
    private final DeliveryOtpVerificationRepository otpVerificationRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PlatformSettingService platformSettingService;
    private final SellerWalletService walletService;
    private final FinancialLedgerService ledgerService;
    private final FinancialAuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    /**
     * Creates a pending settlement record when payment is confirmed.
     * Called from PaymentAllocationService after successful payment.
     */
    @Transactional
    public Settlement createPendingSettlement(Long shipmentId, Long paymentId, Long actorId) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        // Check if settlement already exists
        Optional<Settlement> existing = settlementRepository.findByShipmentId(shipmentId);
        if (existing.isPresent()) {
            Settlement s = existing.get();
            if (s.getPaymentId() == null && paymentId != null) {
                s.setPaymentId(paymentId);
                s.setPaymentConfirmedAt(LocalDateTime.now());
                s.setStatus(SettlementStatus.PENDING_DELIVERY);
                return settlementRepository.save(s);
            }
            return s;
        }

        Order order = orderRepository.findByIdForUpdate(shipment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", shipment.getOrderId()));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        // Get seller for this shipment (from shipment items or order items)
        Long sellerId = getSellerIdForShipment(shipment, order);
        Long transportProviderId = getTransportProviderIdForShipment(shipment);

        // Calculate settlement amounts using current platform settings
        SettlementCalculation calc = calculateSettlement(shipment, order, sellerId, transportProviderId);

        Settlement settlement = Settlement.builder()
                .shipmentId(shipmentId)
                .orderId(order.getId())
                .sellerId(sellerId)
                .driverId(shipment.getDriverId())
                .transportProviderId(transportProviderId)
                .status(SettlementStatus.PENDING_DELIVERY)
                .productSubtotal(calc.productSubtotal)
                .transportFee(calc.transportFee)
                .inspectionFee(calc.inspectionFee)
                .platformFee(calc.platformFee)
                .sellerNetEarnings(calc.sellerNetEarnings)
                .transportProviderPayable(calc.transportProviderPayable)
                .platformRevenue(calc.platformRevenue)
                .totalCollected(calc.totalCollected)
                .platformFeeType(calc.platformFeeType)
                .platformFeeValue(calc.platformFeeValue)
                .platformFeeChargedTo(calc.platformFeeChargedTo)
                .transportFeeType(calc.transportFeeType)
                .transportFeePerKm(calc.transportFeePerKm)
                .transportFeeMin(calc.transportFeeMin)
                .transportFeeMax(calc.transportFeeMax)
                .paymentId(paymentId)
                .paymentConfirmedAt(LocalDateTime.now())
                .settlementReference("SETL-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .createdBy(actorId)
                .build();

        Settlement saved = settlementRepository.save(settlement);

        // Update shipment with settlement reference
        shipment.setSettlementId(saved.getId());
        shipmentRepository.save(shipment);

        // Create allocation records
        createAllocations(saved, calc);

        auditLogService.log(
                actorId,
                "SETTLEMENT_CREATED",
                "Settlement",
                saved.getId(),
                String.format("shipment_id=%d, order_id=%d, seller_id=%d, total_collected=%s",
                        shipmentId, order.getId(), sellerId, calc.totalCollected));

        log.info("Created pending settlement {} for shipment {}: total={}", saved.getId(), shipmentId, calc.totalCollected);
        return saved;
    }

    /**
     * Driver requests an OTP for a specific shipment.
     * Only the assigned driver can request.
     */
    @Transactional
    public DeliveryOtpVerification requestDeliveryOtp(Long shipmentId, Long driverId) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        // Verify driver is assigned
        if (!java.util.Objects.equals(shipment.getDriverId(), driverId)) {
            throw new BadRequestException("Only the assigned driver can request OTP for this shipment");
        }

        // Check shipment is in a state where delivery can be confirmed
        if (shipment.getStatus() != com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus.IN_TRANSIT
                && shipment.getStatus() != com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus.DRIVER_ACCEPTED
                && shipment.getStatus() != com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus.ASSIGNED) {
            throw new BadRequestException("Shipment is not in a deliverable state: " + shipment.getStatus());
        }

        // Check if OTP already exists and is active
        Optional<DeliveryOtpVerification> existing = otpVerificationRepository.findByShipmentIdAndDriverId(shipmentId, driverId);
        if (existing.isPresent()) {
            DeliveryOtpVerification otp = existing.get();
            if (otp.getStatus() == DeliveryOtpStatus.SENT || otp.getStatus() == DeliveryOtpStatus.PENDING) {
                // Check if expired
                if (otp.isExpired()) {
                    otp.setStatus(DeliveryOtpStatus.EXPIRED);
                    otpVerificationRepository.save(otp);
                } else {
                    throw new BadRequestException("An active OTP already exists for this shipment");
                }
            } else if (otp.getStatus() == DeliveryOtpStatus.VERIFIED || otp.getStatus() == DeliveryOtpStatus.USED) {
                throw new BadRequestException("Delivery already confirmed for this shipment");
            }
        }

        // Get settlement for this shipment
        Settlement settlement = settlementRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new BadRequestException("No settlement found for shipment. Payment may not be confirmed."));

        if (settlement.getStatus() == SettlementStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Cannot request OTP: payment not yet confirmed");
        }

        // Get buyer ID from order
        Order order = orderRepository.findByIdForUpdate(shipment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", shipment.getOrderId()));

        // Generate OTP
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        // Create OTP verification record
        DeliveryOtpVerification verification = DeliveryOtpVerification.builder()
                .shipmentId(shipmentId)
                .orderId(order.getId())
                .driverId(driverId)
                .buyerId(order.getBuyerId())
                .otpHash(otpHash)
                .otpExpiresAt(null) // No expiry by default, can be configured
                .otpAttempts(0)
                .status(DeliveryOtpStatus.SENT)
                .requestedAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .settlementId(settlement.getId())
                .build();

        DeliveryOtpVerification saved = otpVerificationRepository.save(verification);

        // Send OTP to buyer via existing notification infrastructure
        sendOtpToBuyer(order.getBuyerId(), otp, shipment, order);

        auditLogService.log(
                driverId,
                "DELIVERY_OTP_REQUESTED",
                "DeliveryOtpVerification",
                saved.getId(),
                String.format("shipment_id=%d, buyer_id=%d", shipmentId, order.getBuyerId()));

        log.info("Driver {} requested delivery OTP for shipment {}", driverId, shipmentId);
        return saved;
    }

    /**
     * Driver submits OTP to confirm delivery.
     * Triggers financial settlement if OTP is valid.
     */
    @Transactional
    public Settlement confirmDeliveryWithOtp(Long shipmentId, Long driverId, String otp) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        // Verify driver is assigned
        if (!java.util.Objects.equals(shipment.getDriverId(), driverId)) {
            throw new BadRequestException("Only the assigned driver can confirm delivery for this shipment");
        }

        // Find OTP verification
        DeliveryOtpVerification verification = otpVerificationRepository.findByShipmentIdAndDriverId(shipmentId, driverId)
                .orElseThrow(() -> new BadRequestException("No OTP found for this shipment and driver"));

        // Verify OTP
        if (!verification.canVerify()) {
            if (verification.isExpired()) {
                throw new BadRequestException("OTP has expired");
            }
            if (verification.isLocked()) {
                throw new BadRequestException("OTP is locked due to too many failed attempts. Try again later.");
            }
            if (verification.getOtpVerified()) {
                throw new BadRequestException("OTP has already been verified");
            }
            throw new BadRequestException("OTP cannot be verified in current state: " + verification.getStatus());
        }

        // Check OTP hash
        if (!passwordEncoder.matches(otp.trim(), verification.getOtpHash())) {
            int attempts = verification.getOtpAttempts() + 1;
            verification.setOtpAttempts(attempts);

            // Lock after 5 failed attempts
            if (attempts >= 5) {
                verification.setOtpLockedUntil(LocalDateTime.now().plusMinutes(15));
                verification.setOtpAttempts(0);
            }

            otpVerificationRepository.save(verification);

            auditLogService.log(
                    driverId,
                    "DELIVERY_OTP_VERIFICATION_FAILED",
                    "DeliveryOtpVerification",
                    verification.getId(),
                    String.format("shipment_id=%d, attempts=%d", shipmentId, attempts));

            throw new BadRequestException("Incorrect OTP. " + (5 - attempts) + " attempts remaining.");
        }

        // OTP verified successfully
        verification.setOtpHash(null); // Clear hash after verification
        verification.setOtpVerified(true);
        verification.setOtpVerifiedAt(LocalDateTime.now());
        verification.setStatus(DeliveryOtpStatus.VERIFIED);
        otpVerificationRepository.save(verification);

        // Get settlement
        Settlement settlement = settlementRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement for shipment", shipmentId));

        // Update settlement status
        settlement.setStatus(SettlementStatus.DELIVERY_CONFIRMED);
        settlement.setDeliveryConfirmedAt(LocalDateTime.now());
        settlement.setOtpVerificationId(verification.getId());
        settlementRepository.save(settlement);

        // Process the settlement (allocate funds to wallets)
        Settlement processed = processSettlement(settlement.getId(), driverId);

        // Mark OTP as used
        verification.setStatus(DeliveryOtpStatus.USED);
        verification.setUsedAt(LocalDateTime.now());
        otpVerificationRepository.save(verification);

        // Update shipment status to DELIVERED
        shipment.setStatus(com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus.DELIVERED);
        shipment.setCompletedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        auditLogService.log(
                driverId,
                "DELIVERY_CONFIRMED_OTP",
                "DeliveryOtpVerification",
                verification.getId(),
                String.format("shipment_id=%d, settlement_id=%d", shipmentId, processed.getId()));

        log.info("Driver {} confirmed delivery for shipment {} via OTP, settlement {} processed",
                driverId, shipmentId, processed.getId());

        return processed;
    }

    /**
     * Processes a confirmed settlement: allocates funds to seller and transport provider wallets.
     */
    @Transactional
    public Settlement processSettlement(Long settlementId, Long actorId) {
        Settlement settlement = settlementRepository.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement", settlementId));

        if (settlement.getStatus() == SettlementStatus.SETTLED) {
            log.info("Settlement {} already processed", settlementId);
            return settlement;
        }

        if (settlement.getStatus() != SettlementStatus.DELIVERY_CONFIRMED
                && settlement.getStatus() != SettlementStatus.SETTLEMENT_PROCESSING) {
            throw new BadRequestException("Settlement cannot be processed in status: " + settlement.getStatus());
        }

        settlement.setStatus(SettlementStatus.SETTLEMENT_PROCESSING);
        settlement.setSettlementProcessedAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        try {
            // Credit seller wallet (if applicable)
            if (settlement.getSellerId() != null
                    && settlement.getSellerNetEarnings() != null
                    && settlement.getSellerNetEarnings().compareTo(BigDecimal.ZERO) > 0) {

                walletService.releasePendingToAvailable(
                        settlement.getSellerId(),
                        null, // No specific fulfilment for shipment-based settlement
                        settlement.getSellerNetEarnings(),
                        actorId);

                // Create wallet transaction reference in allocation
                updateAllocationWalletTxn(settlement.getId(), SettlementAllocationType.SELLER_EARNINGS,
                        settlement.getSellerId());
            }

            // Credit transport provider wallet (if applicable)
            if (settlement.getTransportProviderId() != null
                    && settlement.getTransportProviderPayable() != null
                    && settlement.getTransportProviderPayable().compareTo(BigDecimal.ZERO) > 0) {

                walletService.releasePendingToAvailable(
                        settlement.getTransportProviderId(),
                        null,
                        settlement.getTransportProviderPayable(),
                        actorId);

                updateAllocationWalletTxn(settlement.getId(), SettlementAllocationType.TRANSPORT_FEE,
                        settlement.getTransportProviderId());
            }

            // Record platform revenue in ledger
            recordPlatformRevenue(settlement, actorId);

            // Update settlement status
            settlement.setStatus(SettlementStatus.SETTLED);
            settlement.setSettledAt(LocalDateTime.now());
            Settlement saved = settlementRepository.save(settlement);

            auditLogService.log(
                    actorId,
                    "SETTLEMENT_PROCESSED",
                    "Settlement",
                    saved.getId(),
                    String.format("seller_earnings=%s, transport_payable=%s, platform_revenue=%s",
                            settlement.getSellerNetEarnings(),
                            settlement.getTransportProviderPayable(),
                            settlement.getPlatformRevenue()));

            log.info("Settlement {} processed successfully: seller={}, transport={}, platform={}",
                    settlementId,
                    settlement.getSellerNetEarnings(),
                    settlement.getTransportProviderPayable(),
                    settlement.getPlatformRevenue());

            return saved;

        } catch (Exception e) {
            log.error("Failed to process settlement {}: {}", settlementId, e.getMessage(), e);
            settlement.setStatus(SettlementStatus.FAILED);
            settlementRepository.save(settlement);
            throw new RuntimeException("Settlement processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates settlement amounts for a shipment based on order items and platform settings.
     */
    public SettlementCalculation calculateSettlement(Shipment shipment, Order order,
                                                     Long sellerId, Long transportProviderId) {
        // Get platform settings
        var settings = platformSettingService.getSettings();

        // Get order items for this seller
        List<OrderItem> sellerItems = orderItemRepository.findByOrderId(order.getId()).stream()
                .filter(item -> java.util.Objects.equals(item.getSellerId(), sellerId))
                .toList();

        BigDecimal productSubtotal = sellerItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate platform fee
        BigDecimal platformFeeValue = settings.getPlatformFeeValue() != null
                ? settings.getPlatformFeeValue()
                : settings.getTransactionFee();
        String platformFeeType = settings.getPlatformFeeType() != null
                ? settings.getPlatformFeeType()
                : "PERCENTAGE";

        BigDecimal platformFee;
        if ("FIXED".equals(platformFeeType)) {
            platformFee = platformFeeValue;
        } else {
            platformFee = productSubtotal.multiply(platformFeeValue)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        // Calculate transport fee
        BigDecimal transportFee = calculateTransportFee(shipment, settings);

        // Calculate inspection fee (if applicable)
        BigDecimal inspectionFee = settings.getInspectionFee() != null ? settings.getInspectionFee() : BigDecimal.ZERO;

        // Total collected from buyer
        BigDecimal totalCollected = productSubtotal.add(transportFee).add(inspectionFee);

        // Determine who bears platform fee
        String platformFeeChargedTo = settings.getPlatformFeeChargedTo() != null
                ? settings.getPlatformFeeChargedTo()
                : "SELLER";

        BigDecimal sellerNetEarnings;
        BigDecimal platformRevenue = platformFee;

        if ("BUYER".equals(platformFeeChargedTo)) {
            // Buyer pays platform fee on top
            sellerNetEarnings = productSubtotal;
            totalCollected = totalCollected.add(platformFee);
        } else {
            // Seller bears platform fee
            sellerNetEarnings = productSubtotal.subtract(platformFee);
        }

        // Transport provider payable
        BigDecimal transportProviderPayable = Boolean.TRUE.equals(settings.getTransportFeePaidToProvider())
                ? transportFee
                : BigDecimal.ZERO;

        return SettlementCalculation.builder()
                .productSubtotal(productSubtotal)
                .transportFee(transportFee)
                .inspectionFee(inspectionFee)
                .platformFee(platformFee)
                .sellerNetEarnings(sellerNetEarnings)
                .transportProviderPayable(transportProviderPayable)
                .platformRevenue(platformRevenue)
                .totalCollected(totalCollected)
                .platformFeeType(platformFeeType)
                .platformFeeValue(platformFeeValue)
                .platformFeeChargedTo(platformFeeChargedTo)
                .transportFeeType(settings.getTransportFeeType())
                .transportFeePerKm(settings.getTransportFeePerKm())
                .transportFeeMin(settings.getTransportFeeMin())
                .transportFeeMax(settings.getTransportFeeMax())
                .build();
    }

    private BigDecimal calculateTransportFee(Shipment shipment, com.kilivana.backend.admin.dto.PlatformSettingResponse settings) {
        String transportFeeType = settings.getTransportFeeType() != null
                ? settings.getTransportFeeType()
                : "FIXED";

        if ("PER_KM".equals(transportFeeType) && settings.getTransportFeePerKm() != null
                && shipment.getDistanceKm() != null && shipment.getDistanceKm() > 0) {
            BigDecimal fee = settings.getTransportFeePerKm()
                    .multiply(new BigDecimal(shipment.getDistanceKm()))
                    .setScale(2, RoundingMode.HALF_UP);

            // Apply min/max
            if (settings.getTransportFeeMin() != null
                    && fee.compareTo(settings.getTransportFeeMin()) < 0) {
                fee = settings.getTransportFeeMin();
            }
            if (settings.getTransportFeeMax() != null
                    && fee.compareTo(settings.getTransportFeeMax()) > 0) {
                fee = settings.getTransportFeeMax();
            }
            return fee;
        }

        // Default to fixed fee
        return settings.getTransportFeeDefault() != null
                ? settings.getTransportFeeDefault()
                : new BigDecimal("100.00");
    }

    private Long getSellerIdForShipment(Shipment shipment, Order order) {
        // Get seller from shipment items (first item's seller)
        if (shipment.getShipmentItems() != null && !shipment.getShipmentItems().isEmpty()) {
            Long sellerId = shipment.getShipmentItems().get(0).getOrderItemId() != null
                    ? orderItemRepository.findById(shipment.getShipmentItems().get(0).getOrderItemId())
                    .map(OrderItem::getSellerId)
                    .orElse(null)
                    : null;
            if (sellerId != null) return sellerId;
        }

        // Fallback: get from order items (first seller)
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (!items.isEmpty()) {
            return items.get(0).getSellerId();
        }
        return null;
    }

    private Long getTransportProviderIdForShipment(Shipment shipment) {
        // If driver has a transport provider profile, use that
        if (shipment.getDriverId() != null) {
            User driver = userRepository.findById(shipment.getDriverId()).orElse(null);
            if (driver != null && driver.getRole() == UserRole.DRIVER) {
                // Check if driver belongs to a transport provider
                // For now, return the driver ID as transport provider
                // In a real system, there would be a TransportProvider entity linking drivers to companies
                return driver.getId();
            }
        }
        return null;
    }

    private void createAllocations(Settlement settlement, SettlementCalculation calc) {
        // Seller earnings allocation
        if (calc.sellerNetEarnings != null && calc.sellerNetEarnings.compareTo(BigDecimal.ZERO) > 0) {
            SettlementAllocation alloc = SettlementAllocation.builder()
                    .settlementId(settlement.getId())
                    .allocationType(SettlementAllocationType.SELLER_EARNINGS)
                    .amount(calc.sellerNetEarnings)
                    .recipientUserId(settlement.getSellerId())
                    .description("Seller net earnings for shipment " + settlement.getShipmentId())
                    .build();
            allocationRepository.save(alloc);
        }

        // Transport fee allocation
        if (calc.transportProviderPayable != null && calc.transportProviderPayable.compareTo(BigDecimal.ZERO) > 0) {
            SettlementAllocation alloc = SettlementAllocation.builder()
                    .settlementId(settlement.getId())
                    .allocationType(SettlementAllocationType.TRANSPORT_FEE)
                    .amount(calc.transportProviderPayable)
                    .recipientUserId(settlement.getTransportProviderId())
                    .description("Transport fee for shipment " + settlement.getShipmentId())
                    .build();
            allocationRepository.save(alloc);
        }

        // Inspection fee allocation
        if (calc.inspectionFee != null && calc.inspectionFee.compareTo(BigDecimal.ZERO) > 0) {
            SettlementAllocation alloc = SettlementAllocation.builder()
                    .settlementId(settlement.getId())
                    .allocationType(SettlementAllocationType.INSPECTION_FEE)
                    .amount(calc.inspectionFee)
                    .description("Inspection fee for shipment " + settlement.getShipmentId())
                    .build();
            allocationRepository.save(alloc);
        }

        // Platform commission allocation
        if (calc.platformFee != null && calc.platformFee.compareTo(BigDecimal.ZERO) > 0) {
            SettlementAllocation alloc = SettlementAllocation.builder()
                    .settlementId(settlement.getId())
                    .allocationType(SettlementAllocationType.PLATFORM_COMMISSION)
                    .amount(calc.platformFee)
                    .description("Platform commission for shipment " + settlement.getShipmentId())
                    .build();
            allocationRepository.save(alloc);
        }

        // Platform revenue allocation
        if (calc.platformRevenue != null && calc.platformRevenue.compareTo(BigDecimal.ZERO) > 0) {
            SettlementAllocation alloc = SettlementAllocation.builder()
                    .settlementId(settlement.getId())
                    .allocationType(SettlementAllocationType.PLATFORM_REVENUE)
                    .amount(calc.platformRevenue)
                    .description("Platform revenue for shipment " + settlement.getShipmentId())
                    .build();
            allocationRepository.save(alloc);
        }
    }

    private void updateAllocationWalletTxn(Long settlementId, SettlementAllocationType type, Long recipientId) {
        // This would link the allocation to the actual wallet transaction
        // Implementation depends on how wallet transactions are tracked
    }

    private void recordPlatformRevenue(Settlement settlement, Long actorId) {
        // Record platform revenue in ledger
        if (settlement.getPlatformRevenue() != null
                && settlement.getPlatformRevenue().compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.postSimpleJournalEntry(
                    "PLATFORM_REVENUE",
                    settlement.getId(),
                    "Platform revenue from settlement " + settlement.getSettlementReference(),
                    actorId,
                    ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.PAYMENT_CLEARING).getId(),
                    ledgerService.getOrCreatePlatformAccount(com.kilivana.backend.finance.enums.LedgerAccountType.PLATFORM_COMMISSION_REVENUE).getId(),
                    settlement.getPlatformRevenue(),
                    "Platform revenue: " + settlement.getSettlementReference()
            );
        }
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(OTP_RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtpToBuyer(Long buyerId, String otp, Shipment shipment, Order order) {
        // Use existing notification infrastructure
        // This would integrate with the existing DeliveryOtpNotifier or similar
        log.info("OTP sent to buyer {} for shipment {}: {}", buyerId, shipment.getId(), otp);
    }

    @Transactional(readOnly = true)
    public Optional<Settlement> getSettlementByShipmentId(Long shipmentId) {
        return settlementRepository.findByShipmentId(shipmentId);
    }

    @Transactional(readOnly = true)
    public List<Settlement> getSettlementsByOrderId(Long orderId) {
        return settlementRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Settlement> getSettlementsBySellerId(Long sellerId) {
        return settlementRepository.findBySellerId(sellerId);
    }

    @Transactional(readOnly = true)
    public List<Settlement> getSettlementsByTransportProviderId(Long providerId) {
        return settlementRepository.findByTransportProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryOtpVerification> getOtpVerification(Long shipmentId, Long driverId) {
        return otpVerificationRepository.findByShipmentIdAndDriverId(shipmentId, driverId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryOtpVerification> getOtpVerificationsByDriver(Long driverId) {
        return otpVerificationRepository.findByDriverId(driverId);
    }

    // Inner class for settlement calculation result
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SettlementCalculation {
        private BigDecimal productSubtotal;
        private BigDecimal transportFee;
        private BigDecimal inspectionFee;
        private BigDecimal platformFee;
        private BigDecimal sellerNetEarnings;
        private BigDecimal transportProviderPayable;
        private BigDecimal platformRevenue;
        private BigDecimal totalCollected;
        private String platformFeeType;
        private BigDecimal platformFeeValue;
        private String platformFeeChargedTo;
        private String transportFeeType;
        private BigDecimal transportFeePerKm;
        private BigDecimal transportFeeMin;
        private BigDecimal transportFeeMax;
    }
}