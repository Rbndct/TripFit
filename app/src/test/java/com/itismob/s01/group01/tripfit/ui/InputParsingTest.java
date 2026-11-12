package com.itismob.s01.group01.tripfit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class InputParsingTest {

    @Test
    public void parseAmount_validNumber_returnsValue() {
        assertEquals(1200.5, AddExpenseDialog.parseAmount(" 1200.5 "), 0.0001);
    }

    @Test
    public void parseAmount_blank_returnsNull() {
        assertNull(AddExpenseDialog.parseAmount(""));
    }

    @Test
    public void parseAmount_zeroOrNegative_returnsNull() {
        assertNull(AddExpenseDialog.parseAmount("0"));
        assertNull(AddExpenseDialog.parseAmount("-5"));
    }

    @Test
    public void parseAmount_garbage_returnsNull() {
        assertNull(AddExpenseDialog.parseAmount("12abc"));
    }

    @Test
    public void safeName_stripsSpecialCharacters() {
        assertEquals("tripfit-tokyo-trip", TripPdfWriter.safeName("Tokyo Trip!"));
    }

    @Test
    public void safeName_emptyTitle_fallsBackToDefault() {
        assertEquals("tripfit-itinerary", TripPdfWriter.safeName("???"));
    }
}
