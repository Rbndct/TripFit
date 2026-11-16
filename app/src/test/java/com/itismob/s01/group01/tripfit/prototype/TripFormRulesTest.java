package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TripFormRulesTest {

    @Test
    public void isValidIsoDate_acceptsRealDate() {
        assertTrue(TripFormRules.isValidIsoDate("2026-12-01"));
        assertTrue(TripFormRules.isValidIsoDate("2028-02-29"));
    }

    @Test
    public void isValidIsoDate_rejectsNonexistentAndMalformed() {
        assertFalse(TripFormRules.isValidIsoDate("2026-02-30"));
        assertFalse(TripFormRules.isValidIsoDate("2026-13-01"));
        assertFalse(TripFormRules.isValidIsoDate("2027-02-29"));
        assertFalse(TripFormRules.isValidIsoDate("2026-1-1"));
        assertFalse(TripFormRules.isValidIsoDate("12/01/2026"));
        assertFalse(TripFormRules.isValidIsoDate(" 2026-12-01"));
        assertFalse(TripFormRules.isValidIsoDate(""));
        assertFalse(TripFormRules.isValidIsoDate(null));
    }

    @Test
    public void parseDays_acceptsBoundaries() {
        assertEquals(Integer.valueOf(1), TripFormRules.parseDays("1"));
        assertEquals(Integer.valueOf(30), TripFormRules.parseDays("30"));
        assertEquals(Integer.valueOf(3), TripFormRules.parseDays(" 3 "));
    }

    @Test
    public void parseDays_rejectsOutOfRangeAndGarbage_neverClamps() {
        assertNull(TripFormRules.parseDays("0"));
        assertNull(TripFormRules.parseDays("31"));
        assertNull(TripFormRules.parseDays("-2"));
        assertNull(TripFormRules.parseDays("2.5"));
        assertNull(TripFormRules.parseDays("abc"));
        assertNull(TripFormRules.parseDays(""));
        assertNull(TripFormRules.parseDays(null));
        assertNull(TripFormRules.parseDays("99999999999999"));
    }

    @Test
    public void parseBudget_acceptsBoundaries() {
        assertEquals(1.0, TripFormRules.parseBudget("1"), 0.0);
        assertEquals(100000000.0, TripFormRules.parseBudget("100000000"), 0.0);
        assertEquals(15000.5, TripFormRules.parseBudget("15000.5"), 0.0);
    }

    @Test
    public void parseBudget_rejectsOutOfRangeAndNonFinite() {
        assertNull(TripFormRules.parseBudget("0"));
        assertNull(TripFormRules.parseBudget("0.99"));
        assertNull(TripFormRules.parseBudget("100000000.01"));
        assertNull(TripFormRules.parseBudget("-5"));
        assertNull(TripFormRules.parseBudget("NaN"));
        assertNull(TripFormRules.parseBudget("Infinity"));
        assertNull(TripFormRules.parseBudget("1e400"));
        assertNull(TripFormRules.parseBudget(""));
        assertNull(TripFormRules.parseBudget(null));
    }

    @Test
    public void canShorten_refusesWhenStopsExistOnLaterDays() {
        assertFalse(TripFormRules.canShorten(2, 3));
        assertTrue(TripFormRules.canShorten(3, 3));
        assertTrue(TripFormRules.canShorten(5, 3));
        assertTrue(TripFormRules.canShorten(1, 0));
    }

    @Test
    public void parseBudget_rejectsNonPlainNumberForms() {
        assertNull(TripFormRules.parseBudget("1e5"));
        assertNull(TripFormRules.parseBudget("15000f"));
        assertNull(TripFormRules.parseBudget("15000d"));
        assertNull(TripFormRules.parseBudget("0x10"));
        assertNull(TripFormRules.parseBudget("1,000"));
        assertNull(TripFormRules.parseBudget(".5"));
        assertNull(TripFormRules.parseBudget("+5"));
    }
}
