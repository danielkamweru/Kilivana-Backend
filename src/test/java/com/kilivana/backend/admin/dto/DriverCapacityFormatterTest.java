package com.kilivana.backend.admin.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The admin panel writes a capacity as "5T" or "200kg" while logistics stores kilograms, so
 * every spelling that panel uses has to survive the round trip.
 */
class DriverCapacityFormatterTest {

    @Test
    void parsesTonnes() {
        assertEquals(5000, DriverCapacityFormatter.toKilograms("5T"));
        assertEquals(5000, DriverCapacityFormatter.toKilograms("5t"));
        assertEquals(2000, DriverCapacityFormatter.toKilograms("2 tonnes"));
    }

    @Test
    void parsesKilograms() {
        assertEquals(200, DriverCapacityFormatter.toKilograms("200kg"));
        assertEquals(200, DriverCapacityFormatter.toKilograms("200 KG"));
        assertEquals(200, DriverCapacityFormatter.toKilograms("200"));
    }

    @Test
    void blankIsAbsentRatherThanZero() {
        // Null means "not recorded". Zero would read as "this driver can carry nothing",
        // which is a different and much worse claim.
        assertNull(DriverCapacityFormatter.toKilograms(null));
        assertNull(DriverCapacityFormatter.toKilograms("   "));
        assertNull(DriverCapacityFormatter.format(null));
    }

    @Test
    void rejectsAnUnreadableCapacity() {
        assertThrows(IllegalArgumentException.class, () -> DriverCapacityFormatter.toKilograms("large"));
    }

    @Test
    void rendersWholeTonnesWithoutTheUnitSpelledOut() {
        assertEquals("5T", DriverCapacityFormatter.format(5000));
        assertEquals("200kg", DriverCapacityFormatter.format(200));
    }
}