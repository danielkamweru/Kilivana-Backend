package com.kilivana.backend.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentMethodTest {

    @ParameterizedTest
    @CsvSource({
            "mpesa, MPESA",
            "MPESA, MPESA",
            "M-PESA, MPESA",
            "m-pesa, MPESA",
            "bank, BANK",
            "BANK_TRANSFER, BANK",
            "Bank Transfer, BANK",
            "card, CARD",
            "CREDIT_CARD, CARD",
            "  mpesa  , MPESA",
    })
    @DisplayName("reads the rail the admin panel actually sends")
    void parsesWhatThePanelSends(String raw, PaymentMethod expected) {
        assertEquals(expected, PaymentMethod.from(raw));
    }

    @ParameterizedTest
    @CsvSource({
            // Written by earlier seeds while the platform was configured for Ghana.
            "MTN_MOMO, MPESA",
            "MTN MoMo, MPESA",
            "MOBILE_MONEY, MPESA",
            "VODACOM, MPESA",
    })
    @DisplayName("translates the older provider names to the Kenyan rail")
    void translatesLegacyProviders(String raw, PaymentMethod expected) {
        assertEquals(expected, PaymentMethod.from(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bitcoin", "cash", "mpesa2", ""})
    @DisplayName("refuses a rail it does not recognise instead of inventing one")
    void refusesUnknownRails(String raw) {
        assertNull(PaymentMethod.from(raw));
    }

    @Test
    void missingRailIsRefusedRatherThanDefaulted() {
        assertNull(PaymentMethod.from(null));
        assertNull(PaymentMethod.from("   "));
    }

    @Test
    @DisplayName("only a payment that has cleared can be refunded")
    void refundableStates() {
        assertTrue(PaymentStatus.PAID.isRefundable());
        assertTrue(PaymentStatus.HELD.isRefundable());
        assertTrue(PaymentStatus.SETTLED.isRefundable());

        assertFalse(PaymentStatus.PENDING.isRefundable());
        assertFalse(PaymentStatus.REFUNDED.isRefundable());
    }

    @Test
    @DisplayName("the status set is exactly what the panel renders")
    void statusSetMatchesThePanel() {
        assertEquals(
                java.util.Set.of("PENDING", "PAID", "HELD", "SETTLED", "REFUNDED"),
                java.util.Arrays.stream(PaymentStatus.values()).map(Enum::name)
                        .collect(java.util.stream.Collectors.toSet()));
    }
}