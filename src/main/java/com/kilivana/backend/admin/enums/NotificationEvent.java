package com.kilivana.backend.admin.enums;

/**
 * Business events that can trigger an optional admin notification.
 * Each event can be toggled on/off via {@link com.kilivana.backend.admin.entity.NotificationPreference}.
 */
public enum NotificationEvent {
    NEW_FARMER_REGISTRATION,
    KYC_SUBMITTED,
    DISPUTE_OPENED,
    ORDER_PLACED,
    PAYOUT_PROCESSED
}