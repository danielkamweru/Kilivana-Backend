package com.kilivana.backend.security;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.*;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.ecommerce.repository.*;
import com.kilivana.backend.farm.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Service-boundary authorization. Supplied IDs select records, never establish identity. */
@Component("resourceAuthorization")
@RequiredArgsConstructor
public class ResourceAuthorization {
    private final UserRepository users;
    private final InspectionRepository inspections;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final DisputeRepository disputes;
    private final CartRepository carts;
    private final NotificationRepository notifications;
    private final com.kilivana.backend.logistics.repository.LogisticsJobRepository jobs;
    private final FarmRepository farms;

    private User actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long id)) return null;
        return users.findById(id).filter(u -> AccountAccessValidator.permitsAuthentication(u.getStatus())).orElse(null);
    }

    public boolean admin() { User u = actor(); return u != null && u.getRole() == UserRole.ADMIN; }
    public boolean self(Long id) {
        User u = actor();
        return u != null && (u.getRole() == UserRole.ADMIN || Objects.equals(u.getId(), id));
    }
    public boolean buyer(Long id) {
        User u = actor();
        return u != null && (u.getRole() == UserRole.ADMIN ||
                (u.getRole() == UserRole.BUYER && Objects.equals(u.getId(), id)));
    }
    public boolean seller(Long id) {
        User u = actor();
        return u != null && (u.getRole() == UserRole.ADMIN || (Objects.equals(u.getId(), id) &&
                (u.getRole() == UserRole.FARMER || u.getRole() == UserRole.SUPPLIER)));
    }
    public boolean inspector(Long id) {
        User u = actor();
        return u != null && (u.getRole() == UserRole.ADMIN ||
                (u.getRole() == UserRole.INSPECTOR && Objects.equals(u.getId(), id)));
    }
    public boolean inspection(Long id) {
        return admin() || inspections.findById(id).map(i -> inspector(i.getInspectorId())).orElse(false);
    }
    /** Whole orders may contain several sellers: do not reveal other sellers' lines. */
    public boolean order(Long id) {
        return admin() || orders.findById(id).map(o -> buyer(o.getBuyerId())).orElse(false);
    }
    /** Participant predicate is for disputes, not permission to read an entire mixed order. */
    public boolean orderParticipant(Long id) {
        User u = actor();
        if (u == null) return false;
        if (u.getRole() == UserRole.ADMIN) return true;
        return orders.findById(id).map(o -> Objects.equals(o.getBuyerId(), u.getId()) ||
                orderItems.findByOrderId(id).stream().anyMatch(i -> Objects.equals(i.getSellerId(), u.getId())))
                .orElse(false);
    }
    /** STK initiation remains seller scoped; supplied identity must match the authenticated actor. */
    public boolean sellerPayment(Long userId, UserRole role, Long orderId) {
        User u = actor();
        return u != null && Objects.equals(u.getId(), userId) && u.getRole() == role &&
                (role == UserRole.FARMER || role == UserRole.SUPPLIER) &&
                orderItems.findByOrderId(orderId).stream()
                        .anyMatch(i -> Objects.equals(i.getSellerId(), u.getId()));
    }

    public boolean payment(Long id) {
        return admin() || payments.findById(id).map(p -> order(p.getOrderId())).orElse(false);
    }
    public boolean dispute(Long id) {
        return admin() || disputes.findById(id).map(d -> self(d.getRaisedBy()) || order(d.getOrderId())).orElse(false);
    }
    public boolean cart(Long id) {
        return admin() || carts.findById(id).map(c -> buyer(c.getBuyerId())).orElse(false);
    }
    public boolean notification(Long id) {
        return admin() || notifications.findById(id).map(n -> self(n.getUserId())).orElse(false);
    }
    public boolean driver(Long id) {
        User u = actor();
        return u != null && (u.getRole() == UserRole.ADMIN ||
                (u.getRole() == UserRole.DRIVER && Objects.equals(u.getId(), id)));
    }
    public boolean jobWrite(Long id) {
        User u = actor();
        if (u == null) return false;
        return u.getRole() == UserRole.ADMIN || jobs.findById(id).map(j ->
                u.getRole() == UserRole.DRIVER && Objects.equals(j.getDriverId(), u.getId())).orElse(false);
    }
    public boolean jobRead(Long id) {
        return jobWrite(id) || jobs.findById(id).map(j -> order(j.getOrderId())).orElse(false);
    }
    public boolean farm(Long id) {
        User u = actor();
        if (u == null) return false;
        if (u.getRole() == UserRole.ADMIN) return true;
        return farms.findById(id).map(f -> u.getRole() == UserRole.FARMER && Objects.equals(f.getFarmerId(), u.getId())).orElse(false);
    }
}
