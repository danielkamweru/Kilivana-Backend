package com.kilivana.backend.admin.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The admin panel's driver form submits one flat object; these are the spellings it uses for the
 * capacity.
 */
class DriverCapacityFormatterTest {

    @Test
    void parsesTonnes() {
        assertEquals(5000, DriverCapacityFormatter.toKilograms("5T"));
        assertEquals(5000, DriverCapacityFormatter.toKilograms("5t"));
        assertEquals(2000, DriverCapacityFormatter.toKilograms("2 tonnes"));
        assertEquals(10000, DriverCapacityFormatter.toKilograms("10 T"));
    }

    @Test
    void parsesKilograms() {
        assertEquals(200, DriverCapacityFormatter.toKilograms("200kg"));
        assertEquals(200, DriverCapacityFormatter.toKilograms("200 KG"));
        assertEquals(2000, DriverCapacityFormatter.toKilograms("2000"));
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
        assertThrows(IllegalArgumentException.class, () -> DriverCapacityFormatter.toKilograms("5 tonnes extra"));
    }

    @Test
    void rendersWholeTonnesWithoutTheUnitSpelledOut() {
        assertEquals("5T", DriverCapacityFormatter.format(5000));
        assertEquals("200kg", DriverCapacityFormatter.format(200));
    }
}