package com.kilivana.backend.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KenyaLocaleTest {

    @Test
    @DisplayName("there are 47 counties, in the official order")
    void countyListIsComplete() {
        assertEquals(47, KenyaCounty.all().size());
        assertEquals("Mombasa", KenyaCounty.all().get(0));
        assertEquals("Nairobi City", KenyaCounty.all().get(46));
        // The panel's own list is alphabetical and omits none; the count is what matters.
        assertEquals(47, KenyaCounty.all().stream().distinct().count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Nairobi", "nairobi", "NAIROBI", "Nairobi City", "  Nairobi  "})
    @DisplayName("Nairobi and Nairobi City are the same place")
    void nairobiSpelling(String raw) {
        assertEquals("Nairobi City", KenyaCounty.normalise(raw));
    }

    @Test
    @DisplayName("a typographic apostrophe is the same county as a straight one")
    void apostropheVariants() {
        assertEquals("Murang'a", KenyaCounty.normalise("Murang’a"));
        assertEquals("Murang'a", KenyaCounty.normalise("muranga"));
        assertEquals("Murang'a", KenyaCounty.normalise("MURANG'A"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kiambu", "kisumu", "KILIFI", "Uasin Gishu", "Taita-Taveta"})
    void acceptsRealCountiesRegardlessOfCase(String raw) {
        assertTrue(KenyaCounty.isValid(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Ghanaian regions, which the older seed data and rows still carry.
            "Greater Accra", "Ashanti", "Northern", "Volta", "Bono", "Oti",
    })
    @DisplayName("a Ghanaian region is not a Kenyan county")
    void rejectsGhanaRegions(String raw) {
        assertFalse(KenyaCounty.isValid(raw));
        assertNull(KenyaCounty.normalise(raw));
    }

    @Test
    void blankRegionIsRefused() {
        assertNull(KenyaCounty.normalise(null));
        assertNull(KenyaCounty.normalise("   "));
    }

    @Test
    @DisplayName("the driver document list is the Kenyan one")
    void idTypesAreKenyan() {
        assertEquals(java.util.List.of("National ID", "Passport", "Driving Licence"),
                KenyanIdType.all());
        assertFalse(KenyanIdType.all().contains("Ghana Card"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"National ID", "national id", "NationalID", "ID"})
    @DisplayName("the national identity card is spelled several ways")
    void nationalIdVariants(String raw) {
        assertEquals("National ID", KenyanIdType.normalise(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Driving Licence", "driving licence", "Drivers Licence", "licence"})
    void drivingLicenceVariants(String raw) {
        assertEquals("Driving Licence", KenyanIdType.normalise(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ghana Card", "Voter ID"})
    @DisplayName("the Ghanaian labels map onto the national ID, not onto a document Kenyan drivers show")
    void ghanaianLabelsMapOntoNationalId(String raw) {
        assertEquals("National ID", KenyanIdType.normalise(raw));
    }

    @Test
    void unknownDocumentIsRefused() {
        assertNull(KenyanIdType.normalise("Passport Card"));
        assertNull(KenyanIdType.normalise(null));
        assertNull(KenyanIdType.normalise("  "));
    }
}