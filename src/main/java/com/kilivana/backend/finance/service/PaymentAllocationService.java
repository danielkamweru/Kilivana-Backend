package com.kilivana.backend.finance.service;

import com.kilivana.backend.admin.service.PlatformSettingService;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment;
import com.kilivana.backend.logistics.fulfilment.enums.FulfilmentStatus;
import com.kilivana.backend.logistics.fulfilment.repository.SellerFulfilmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for allocating successful buyer payments to seller wallets.
 * Calculates commission, creates wallet credits, and posts ledger entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAllocationService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SellerFulfilmentRepository fulfilmentRepository;
    private final SellerWalletService walletService;
    private final PlatformSettingService platformSettingService;

    /**
     * Allocates a successful payment to seller wallets.
     * Called after payment callback confirms PAID status.
     */
    @Transactional
    public void allocatePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Payment " + paymentId + " is not in PAID status: " + payment.getStatus());
        }

        // Check if already allocated
        if (payment.getQuoteId() == null && payment.getOrderId() != null) {
            // For order-based payments, check if seller fulfilments already have credits
            List<SellerFulfilment> fulfilments = fulfilmentRepository.findByOrderId(payment.getOrderId());
            boolean alreadyAllocated = fulfilments.stream()
                    .allMatch(f -> walletService.hasSaleCredit(f.getId()));
            if (alreadyAllocated) {
                log.info("Payment {} already allocated to sellers", paymentId);
                return;
            }
        }

        Order order;
        if (payment.getOrderId() != null) {
            order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order", payment.getOrderId()));
        } else if (payment.getQuoteId() != null) {
            // For quote-based payments, the order should have been created by the callback
            throw new BadRequestException("Quote-based payment allocation not yet supported - order must exist");
        } else {
            throw new BadRequestException("Payment has neither orderId nor quoteId");
        }

        // Get platform commission rate
        BigDecimal commissionRate = platformSettingService.getSettings().getTransactionFee()
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP); // Convert percentage to decimal

        // Get order items grouped by seller
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("Order " + order.getId() + " has no items");
        }

        Map<Long, List<OrderItem>> itemsBySeller = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getSellerId));

        // Get fulfilments for this order
        List<SellerFulfilment> fulfilments = fulfilmentRepository.findByOrderId(order.getId());
        Map<Long, SellerFulfilment> fulfilmentBySeller = fulfilments.stream()
                .collect(Collectors.toMap(SellerFulfilment::getSellerId, f -> f));

        // Get delivery fee allocation policy from settings
        BigDecimal deliveryFee = order.getDeliveryFee();
        BigDecimal totalProductAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Map.Entry<Long, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<OrderItem> sellerItems = entry.getValue();

            BigDecimal sellerSubtotal = sellerItems.stream()
                    .map(OrderItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate seller's share of delivery fee (proportional to product subtotal)
            BigDecimal sellerDeliveryShare = deliveryFee.multiply(sellerSubtotal)
                    .divide(totalProductAmount, 2, RoundingMode.HALF_UP);

            BigDecimal sellerGrossTotal = sellerSubtotal.add(sellerDeliveryShare);

            // Calculate commission on product subtotal only (not delivery)
            BigDecimal commissionAmount = sellerSubtotal.multiply(commissionRate)
                    .setScale(2, RoundingMode.HALF_UP);

            // Net earnings = product subtotal - commission + delivery share
            BigDecimal netEarnings = sellerSubtotal.subtract(commissionAmount).add(sellerDeliveryShare)
                    .setScale(2, RoundingMode.HALF_UP);

            SellerFulfilment fulfilment = fulfilmentBySeller.get(sellerId);
            if (fulfilment == null) {
                throw new BadRequestException("No fulfilment found for seller " + sellerId + " in order " + order.getId());
            }

            // Update fulfilment with calculated amounts
            fulfilment.setSellerSubtotal(sellerGrossTotal);
            fulfilmentRepository.save(fulfilment);

            String reference = "SALE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            String description = String.format("Order %s (Fulfilment %d): subtotal=%s, delivery_share=%s, commission=%s, net=%s",
                    order.getCode(), fulfilment.getId(), sellerSubtotal, sellerDeliveryShare, commissionAmount, netEarnings);

            // Credit seller wallet (pending until delivery)
            walletService.creditSale(
                    sellerId,
                    order.getId(),
                    fulfilment.getId(),
                    sellerGrossTotal,
                    commissionAmount,
                    netEarnings,
                    reference,
                    description
            );

            log.info("Allocated payment {} to seller {}: gross={}, commission={}, net={}",
                    paymentId, sellerId, sellerGrossTotal, commissionAmount, netEarnings);
        }

        // Verify total allocation matches payment amount (within rounding)
        BigDecimal totalAllocated = itemsBySeller.values().stream()
                .flatMap(List::stream)
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(deliveryFee);

        if (totalAllocated.subtract(payment.getAmount()).abs().compareTo(new BigDecimal("0.02")) > 0) {
            log.warn("Payment allocation mismatch: payment={}, allocated={}, diff={}",
                    payment.getAmount(), totalAllocated, payment.getAmount().subtract(totalAllocated));
        }
    }

    /**
     * Checks if a seller fulfilment has already been credited.
     */
    @Transactional(readOnly = true)
    public boolean hasSaleCredit(Long sellerFulfilmentId) {
        return walletService.hasSaleCredit(sellerFulfilmentId);
    }
}