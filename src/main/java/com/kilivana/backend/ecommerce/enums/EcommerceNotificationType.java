package com.kilivana.backend.ecommerce.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Kinds of notification the ecommerce side can send a user, each with a code, a
 * default title and a default message used when no override has been configured.
 */
@Schema(description = "Kinds of notification the ecommerce side can send a user, each with a code, a default title and a default message used when no override has been configured.")
@Getter
public enum EcommerceNotificationType {

    ORDER_PLACED("order.placed", "New Order", "A new order has been placed"),
    ORDER_STATUS_CHANGED("order.status_changed", "Order Update", "Your order status has changed"),
    ORDER_CANCELLED("order.cancelled", "Order Cancelled", "An order was cancelled"),
    ORDER_DISPUTE("order.dispute", "Order Dispute", "A dispute has been raised about an order"),
    PAYMENT_RECEIVED("payment.received", "Payment Received", "Payment has been received"),
    PAYMENT_FAILED("payment.failed", "Payment Failed", "A payment attempt failed"),
    PAYMENT_REFUNDED("payment.refunded", "Refund Processed", "A refund has been processed"),
    DISPUTE_RAISED("dispute.raised", "Dispute Raised", "A new dispute has been filed"),
    DISPUTE_RESOLVED("dispute.resolved", "Dispute Resolved", "A dispute has been resolved"),
    SHIPMENT_CREATED("shipment.created", "Shipment Created", "Your order has been shipped"),
    INVENTORY_LOW("inventory.low", "Low Stock Alert", "A product is running low on stock");

    private final String code;
    private final String defaultTitle;
    private final String defaultMessage;

    EcommerceNotificationType(String code, String defaultTitle, String defaultMessage) {
        this.code = code;
        this.defaultTitle = defaultTitle;
        this.defaultMessage = defaultMessage;
    }
}
