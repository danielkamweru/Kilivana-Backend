package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The driver columns used to be free text and already held both "Truck" and "TRUCK". Lenient
 * parsing is what stops that from starting again.
 */
class DriverEnumParsingTest {

    @Test
    void driverStatusAcceptsEveryClientSpelling() {
        assertEquals(DriverStatus.AVAILABLE, DriverStatus.from("available"));
        assertEquals(DriverStatus.ON_DELIVERY, DriverStatus.from("on-delivery"));
        assertEquals(DriverStatus.ON_DELIVERY, DriverStatus.from("ON_DELIVERY"));
        assertEquals(DriverStatus.OFFLINE, DriverStatus.from(" offline "));
        assertEquals(DriverStatus.SUSPENDED, DriverStatus.from("Suspended"));
    }

    @Test
    void vehicleTypeAcceptsTitleCase() {
        assertEquals(VehicleType.TRUCK, VehicleType.from("Truck"));
        assertEquals(VehicleType.MOTORBIKE, VehicleType.from("motorbike"));
        assertEquals(VehicleType.PICKUP, VehicleType.from("pick-up"));
    }

    @Test
    void kycStatusAcceptsLowerCase() {
        assertEquals(KycStatus.PENDING, KycStatus.from("pending"));
        assertEquals(KycStatus.VERIFIED, KycStatus.from("VERIFIED"));
    }

    @Test
    void absentStaysAbsent() {
        // A field the client did not send must not become a value it never chose.
        assertNull(DriverStatus.from(null));
        assertNull(VehicleType.from(null));
        assertNull(KycStatus.from(null));
    }

    @Test
    void anUnknownValueIsRejectedRatherThanDefaulted() {
        assertThrows(IllegalArgumentException.class, () -> DriverStatus.from("driving"));
        assertThrows(IllegalArgumentException.class, () -> VehicleType.from("Bicycle"));
        assertThrows(IllegalArgumentException.class, () -> KycStatus.from("maybe"));
    }
}