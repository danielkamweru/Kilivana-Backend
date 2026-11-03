package com.kilivana.backend.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Thrown when one or more order items are missing an active dispatch location.
 * Carries per-item details so the admin panel can show exactly which products
 * need a farm or warehouse assigned before the order can be dispatched.
 */
public class DispatchLocationMissingException extends RuntimeException {

    private final Long orderId;
    private final List<Map<String, Object>> invalidItems;

    public DispatchLocationMissingException(Long orderId, List<Map<String, Object>> invalidItems) {
        super("Some ordered products are not ready for dispatch");
        this.orderId = orderId;
        this.invalidItems = invalidItems;
    }

    public Long getOrderId() { return orderId; }
    public List<Map<String, Object>> getInvalidItems() { return invalidItems; }
}
