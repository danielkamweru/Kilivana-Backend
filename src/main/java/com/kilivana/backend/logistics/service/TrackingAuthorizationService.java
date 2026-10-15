package com.kilivana.backend.logistics.service;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Decides who may read a delivery's live position. The driver is always allowed to read
 * their own active job; the buyer and the seller (farmer or supplier) of the order are
 * allowed too; an administrator may read anything. Everyone else is refused, including
 * another driver and a user with no relation to the order.
 *
 * <p>The seller is resolved through the order's line items, because the order itself only
 * stores the buyer. There is no direct order-to-seller link: one order can contain goods
 * from several sellers, and each line records its own.
 */
@Service
@RequiredArgsConstructor
public class TrackingAuthorizationService {

    private final LogisticsJobRepository logisticsJobRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    /**
     * Returns the set of user ids entitled to track this job. Cached per call; the cost is
     * one job lookup, one order lookup and one line-item query, which is cheap enough to
     * run on every read and avoids a second source of truth for who may see what.
     */
    @Transactional(readOnly = true)
    public Set<Long> authorizedViewerIds(Long jobId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ForbiddenException("Delivery not found"));
        Set<Long> ids = new HashSet<>();
        if (job.getDriverId() != null) {
            ids.add(job.getDriverId());
        }
        orderRepository.findById(job.getOrderId()).ifPresent(order -> {
            ids.add(order.getBuyerId());
            for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                ids.add(item.getSellerId());
            }
        });
        return ids;
    }

    /**
     * Throws when the caller may not read this job's tracking. Administrators bypass the
     * relation check entirely.
     */
    @Transactional(readOnly = true)
    public void ensureCanTrack(Long jobId, Long callerId) {
        if (callerId == null) {
            throw new ForbiddenException("Authentication is required to track a delivery");
        }
        if (isAdministrator(callerId)) {
            return;
        }
        if (!authorizedViewerIds(jobId).contains(callerId)) {
            throw new ForbiddenException("You are not authorised to track this delivery");
        }
    }

    /**
     * Throws when the caller is not the driver assigned to the job. Used for the write
     * path, where the buyer and seller are observers, not reporters.
     */
    @Transactional(readOnly = true)
    public void ensureIsAssignedDriver(Long jobId, Long callerId) {
        if (callerId == null) {
            throw new ForbiddenException("Authentication is required to report a location");
        }
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ForbiddenException("Delivery not found"));
        if (job.getDriverId() == null || !job.getDriverId().equals(callerId)) {
            throw new ForbiddenException("Only the driver assigned to this delivery may report a location");
        }
    }

    private boolean isAdministrator(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }
}