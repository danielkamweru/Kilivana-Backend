package com.kilivana.backend.logistics.fulfilment.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.finance.service.FulfilmentWalletService;
import com.kilivana.backend.logistics.fulfilment.entity.SellerFulfilment;
import com.kilivana.backend.logistics.fulfilment.enums.FulfilmentStatus;
import com.kilivana.backend.logistics.fulfilment.repository.SellerFulfilmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and manages seller fulfilment records for multi-farmer orders.
 * <p>
 * A customer order containing products from several farmers produces one
 * {@link SellerFulfilment} per distinct {@code sellerId}. Creation is
 * idempotent: re-creating for the same (order, seller) pair returns the
 * existing record instead of duplicating it.
 * <p>
 * This service is invoked after an order has been created and paid for, both
 * from the payment-first checkout callback and from any admin order flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerFulfilmentService {

    private final SellerFulfilmentRepository fulfilmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final FulfilmentWalletService fulfilmentWalletService;

    /**
     * Creates (or returns existing) seller fulfilment records for every distinct
     * seller in the order. Idempotent: safe to call multiple times for the same
     * order (e.g. retried payment callbacks).
     *
     * @param orderId the paid customer order
     * @return fulfilment records, one per distinct seller
     */
    @Transactional
    public List<SellerFulfilment> ensureFulfilmentsForOrder(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> sellerSubtotals = new LinkedHashMap<>();
        for (OrderItem item : items) {
            sellerSubtotals.merge(
                item.getSellerId(),
                item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal(),
                BigDecimal::add
            );
        }

        List<SellerFulfilment> fulfilments = new java.util.ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : sellerSubtotals.entrySet()) {
            Long sellerId = entry.getKey();
            BigDecimal subtotal = entry.getValue();

            SellerFulfilment existing = fulfilmentRepository.findByOrderIdAndSellerId(orderId, sellerId)
                    .orElseGet(() -> createFulfilment(orderId, sellerId, subtotal));
            fulfilments.add(existing);
        }
        return fulfilments;
    }

    /**
     * Returns all fulfilment records for an order.
     */
    @Transactional(readOnly = true)
    public List<SellerFulfilment> getFulfilmentsForOrder(Long orderId) {
        return fulfilmentRepository.findByOrderId(orderId);
    }

    /**
     * Returns all fulfilment records for a seller (farmer/supplier).
     */
    @Transactional(readOnly = true)
    public List<SellerFulfilment> getFulfilmentsForSeller(Long sellerId) {
        return fulfilmentRepository.findBySellerId(sellerId);
    }

    /**
     * Updates a seller fulfilment's status. Only the owning farmer (or an admin)
     * may transition their own records; enforced by the caller's authorization.
     *
     * @param fulfilmentId the fulfilment record
     * @param status the new status
     * @param updatedBy the user making the change (for audit)
     * @return the updated fulfilment
     */
    @Transactional
    public SellerFulfilment updateStatus(Long fulfilmentId, FulfilmentStatus status, Long updatedBy) {
        SellerFulfilment fulfilment = fulfilmentRepository.findByIdForUpdate(fulfilmentId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerFulfilment", fulfilmentId));

        FulfilmentStatus previous = fulfilment.getStatus();
        if (previous == status) {
            return fulfilment;
        }

        validateTransition(previous, status);

        fulfilment.setStatus(status);
        if (status == FulfilmentStatus.READY_FOR_PICKUP && fulfilment.getReadyAt() == null) {
            fulfilment.setReadyAt(LocalDateTime.now());
        }
        if (status == FulfilmentStatus.COLLECTED && fulfilment.getCollectedAt() == null) {
            fulfilment.setCollectedAt(LocalDateTime.now());
        }

        SellerFulfilment saved = fulfilmentRepository.save(fulfilment);
        log.info("Seller fulfilment {} status {} -> {} (updated by {})",
                fulfilmentId, previous.wire(), status.wire(), updatedBy);

        // Integrate with wallet service for financial lifecycle
        try {
            if (status == FulfilmentStatus.COLLECTED) {
                fulfilmentWalletService.onFulfilmentDelivered(fulfilmentId, updatedBy);
            } else if (status == FulfilmentStatus.CANCELLED) {
                String reason = "Fulfilment cancelled by user " + updatedBy;
                fulfilmentWalletService.onFulfilmentCancelled(fulfilmentId, updatedBy, reason);
            }
        } catch (Exception e) {
            log.error("Failed to process wallet integration for fulfilment {}: {}", fulfilmentId, e.getMessage(), e);
            // Don't fail the status update - wallet integration can be retried
        }

        return saved;
    }

    /**
     * Creates a single fulfilment record. Called only when none exists yet.
     */
    private SellerFulfilment createFulfilment(Long orderId, Long sellerId, BigDecimal sellerSubtotal) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", sellerId));
        if (seller.getRole() != UserRole.FARMER && seller.getRole() != UserRole.SUPPLIER) {
            throw new BadRequestException("User " + sellerId + " is not a farmer or supplier");
        }

        SellerFulfilment fulfilment = SellerFulfilment.builder()
                .orderId(orderId)
                .sellerId(sellerId)
                .sellerSubtotal(sellerSubtotal)
                .status(FulfilmentStatus.PENDING)
                .build();
        SellerFulfilment saved = fulfilmentRepository.save(fulfilment);
        log.info("Created seller fulfilment {} for order {} seller {}", saved.getId(), orderId, sellerId);
        return saved;
    }

    private static void validateTransition(FulfilmentStatus from, FulfilmentStatus to) {
        switch (from) {
            case PENDING -> {
                if (to != FulfilmentStatus.ACCEPTED && to != FulfilmentStatus.CANCELLED) {
                    throw new BadRequestException("A PENDING fulfilment can only be accepted or cancelled");
                }
            }
            case ACCEPTED -> {
                if (to != FulfilmentStatus.PREPARING && to != FulfilmentStatus.CANCELLED) {
                    throw new BadRequestException("An ACCEPTED fulfilment can only move to PREPARING or CANCELLED");
                }
            }
            case PREPARING -> {
                if (to != FulfilmentStatus.READY_FOR_PICKUP && to != FulfilmentStatus.CANCELLED) {
                    throw new BadRequestException("A PREPARING fulfilment can only move to READY_FOR_PICKUP or CANCELLED");
                }
            }
            case READY_FOR_PICKUP -> {
                if (to != FulfilmentStatus.COLLECTED && to != FulfilmentStatus.CANCELLED) {
                    throw new BadRequestException("A READY_FOR_PICKUP fulfilment can only be collected or cancelled");
                }
            }
            case COLLECTED, CANCELLED -> {
                throw new BadRequestException("A " + from.wire() + " fulfilment cannot be changed");
            }
        }
    }
}
