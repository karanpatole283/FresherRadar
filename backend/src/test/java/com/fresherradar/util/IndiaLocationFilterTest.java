
package com.fresherradar.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndiaLocationFilterTest {

    @Test
    void acceptsIndianCities() {
        assertTrue(IndiaLocationFilter.isIndiaBased("Bengaluru, India"));
        assertTrue(IndiaLocationFilter.isIndiaBased("Pune, Maharashtra"));
        assertTrue(IndiaLocationFilter.isIndiaBased("Mumbai"));
        assertTrue(IndiaLocationFilter.isIndiaBased("Hyderabad, Telangana"));
        assertTrue(IndiaLocationFilter.isIndiaBased("Remote - India"));
    }

    @Test
    void rejectsForeignLocations() {
        assertFalse(IndiaLocationFilter.isIndiaBased("London, UK"));
        assertFalse(IndiaLocationFilter.isIndiaBased("New York, USA"));
        assertFalse(IndiaLocationFilter.isIndiaBased("Singapore"));
        assertFalse(IndiaLocationFilter.isIndiaBased("Dubai, UAE"));
    }

    @Test
    void rejectsUnknownOrEmptyLocations() {
        assertFalse(IndiaLocationFilter.isIndiaBased(null));
        assertFalse(IndiaLocationFilter.isIndiaBased(""));
        assertFalse(IndiaLocationFilter.isIndiaBased("Remote"));
        assertFalse(IndiaLocationFilter.isIndiaBased("Worldwide"));
        assertFalse(IndiaLocationFilter.isIndiaBased("Unknown"));
    }

    @Test
    void rejectsForeignLocationEvenIfIndiaIsAlsoMentioned() {
        assertFalse(IndiaLocationFilter.isIndiaBased(
                "Remote - India or United States"
        ));
    }
}
