package com.kilivana.backend.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The wire format is part of the contract: the admin panel sends
 * and compares these exact spellings, so an enum whose JSON name
 * differs from the panel's own union breaks every filter it drives.
 */
class EnumWireFormatTest {

    @ParameterizedTest
    @CsvSource({
            "PLACED, placed",
            "CONFIRMED, confirmed",
            "IN_TRANSIT, in_transit",
            "DELIVERED, delivered",
            "COMPLETED, completed",
            "DISPUTED, disputed",
            "CANCELLED, cancelled",
    })
    @DisplayName("order statuses speak the panel's pipeline")
    void orderStatusWire(OrderStatus constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, OrderStatus.from(wire));
        assertEquals(constant, OrderStatus.from(wire.toUpperCase()));
    }

    @Test
    @DisplayName("the order status set is exactly the panel's pipeline")
    void orderStatusSetMatchesThePanel() {
        assertEquals(java.util.Set.of(
                "PLACED", "CONFIRMED", "IN_TRANSIT", "DELIVERED",
                "COMPLETED", "DISPUTED", "CANCELLED"),
                java.util.Arrays.stream(OrderStatus.values())
                        .map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, pending",
            "PAID, paid",
            "HELD, held",
            "SETTLED, settled",
            "REFUNDED, refunded",
    })
    @DisplayName("payment statuses speak the panel's union")
    void paymentStatusWire(PaymentStatus constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, PaymentStatus.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "MPESA, mpesa",
            "BANK, bank",
            "CARD, card",
    })
    @DisplayName("payment methods speak the panel's union")
    void paymentMethodWire(PaymentMethod constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, PaymentMethod.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, available",
            "ON_DELIVERY, on-delivery",
            "OFFLINE, offline",
            "SUSPENDED, suspended",
    })
    @DisplayName("driver statuses speak the panel's union")
    void driverStatusWire(DriverStatus constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, DriverStatus.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "TRUCK, Truck",
            "VAN, Van",
            "PICKUP, Pickup",
            "MOTORBIKE, Motorbike",
    })
    @DisplayName("vehicle types speak the panel's union")
    void vehicleTypeWire(VehicleType constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, VehicleType.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "FARMER, farmer",
            "SUPPLIER, supplier",
    })
    @DisplayName("seller types speak the panel's union")
    void sellerTypeWire(SellerType constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, SellerType.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, pending",
            "VERIFIED, verified",
    })
    @DisplayName("KYC statuses speak the panel's union")
    void kycStatusWire(KycStatus constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, KycStatus.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "ACTIVE, active",
            "PENDING_APPROVAL, pending",
            "REJECTED, rejected",
            "SUSPENDED, suspended",
    })
    @DisplayName("product statuses speak the panel's union")
    void productStatusWire(ProductStatus constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, ProductStatus.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "ASSIGNED, assigned",
            "PICKED_UP, picked_up",
            "IN_TRANSIT, in_transit",
            "DELIVERED, delivered",
            "CANCELLED, cancelled",
    })
    @DisplayName("delivery statuses speak the panel's union")
    void deliveryStatusWire(DeliveryStatus constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, DeliveryStatus.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, open",
            "IN_PROGRESS, in_progress",
            "RESOLVED, resolved",
            "CLOSED, closed",
    })
    @DisplayName("dispute statuses speak the panel's union")
    void disputeStatusWire(DisputeStatus constant, String wire) {
        assertEquals(wire, constant.wire());
        assertEquals(constant, DisputeStatus.from(wire));
    }

    @ParameterizedTest
    @CsvSource({
            "placed, PLACED",
            "in-transit, IN_TRANSIT",
            "In Transit, IN_TRANSIT",
            "DISPUTED, DISPUTED",
    })
    @DisplayName("order status parsing ignores case and separators")
    void orderStatusParsing(String raw, OrderStatus expected) {
        assertEquals(expected, OrderStatus.from(raw));
    }
}
