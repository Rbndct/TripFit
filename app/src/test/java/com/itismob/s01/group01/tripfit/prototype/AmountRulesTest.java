package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AmountRulesTest {

    @Test
    public void parseExpense_acceptsTwoDecimalsInRange() {
        assertEquals(0.01, AmountRules.parseExpense("0.01"), 0.0);
        assertEquals(1200.5, AmountRules.parseExpense(" 1200.5 "), 0.0);
        assertEquals(100_000_000.0, AmountRules.parseExpense("100000000"), 0.0);
    }

    @Test
    public void parseExpense_rejectsOutOfRangeAndGarbage() {
        assertNull(AmountRules.parseExpense(null));
        assertNull(AmountRules.parseExpense(""));
        assertNull(AmountRules.parseExpense("0"));
        assertNull(AmountRules.parseExpense("0.004"));
        assertNull(AmountRules.parseExpense("-5"));
        assertNull(AmountRules.parseExpense("100000000.01"));
        assertNull(AmountRules.parseExpense("12abc"));
        assertNull(AmountRules.parseExpense("1e5"));
        assertNull(AmountRules.parseExpense("Infinity"));
        assertNull(AmountRules.parseExpense("NaN"));
        assertNull(AmountRules.parseExpense("1.2.3"));
        assertNull(AmountRules.parseExpense("."));
    }

    @Test
    public void parseExpense_roundsToTwoDecimals() {
        assertEquals(1200.56, AmountRules.parseExpense("1200.555"), 0.0);
    }

    @Test
    public void parseOptional_blankMeansZeroAndZeroIsAllowed() {
        assertEquals(0.0, AmountRules.parseOptional(""), 0.0);
        assertEquals(0.0, AmountRules.parseOptional("  "), 0.0);
        assertEquals(0.0, AmountRules.parseOptional("0"), 0.0);
        assertEquals(1e8, AmountRules.parseOptional("100000000"), 0.0);
        assertEquals(12.34, AmountRules.parseOptional("12.34"), 0.0);
    }

    @Test
    public void parseOptional_rejectsTooBigAndGarbage() {
        assertNull(AmountRules.parseOptional("100000000.01"));
        assertNull(AmountRules.parseOptional("-1"));
        assertNull(AmountRules.parseOptional("abc"));
    }

    @Test
    public void parseBudget_mustBePositive() {
        assertNull(AmountRules.parseBudget(""));
        assertNull(AmountRules.parseBudget("0"));
        assertEquals(15000.5, AmountRules.parseBudget("15000.5"), 0.0);
    }

    @Test
    public void format_alwaysTwoDecimalsWithoutGrouping() {
        assertEquals("4500.00", AmountRules.format(4500));
        assertEquals("99.50", AmountRules.format(99.5));
        assertEquals("1234.57", AmountRules.format(1234.567));
        assertEquals("0.00", AmountRules.format(0));
    }

    @Test
    public void fitsWithin_ignoresFloatingPointNoise() {
        assertTrue(AmountRules.fitsWithin(0.1 + 0.2, 0.3));
        assertTrue(AmountRules.fitsWithin(15000, 15000));
        assertFalse(AmountRules.fitsWithin(15000.01, 15000));
    }

    @Test
    public void parse_roundsHalfUpOnTheDecimalText() {
        assertEquals(1.01, AmountRules.parseExpense("1.005"), 0.0);
        assertEquals(2.68, AmountRules.parseExpense("2.675"), 0.0);
        assertEquals(0.01, AmountRules.parseExpense("0.005"), 0.0);
    }

    @Test
    public void parse_acceptsTrailingAndLeadingDot() {
        assertEquals(5.0, AmountRules.parseExpense("5."), 0.0);
        assertEquals(0.5, AmountRules.parseExpense(".5"), 0.0);
    }

    @Test
    public void round2_roundsHalfUpForDoubles() {
        assertEquals(1.01, AmountRules.round2(1.005), 0.0);
        assertEquals(2.68, AmountRules.round2(2.675), 0.0);
    }

    @Test
    public void dailyMode_limitsFitThePerDayAmountNotTheTripTotal() {
        double perDay = 100;
        assertTrue(AmountRules.fitsWithin(30 + 40 + 20 + 10, perDay));
        assertFalse(AmountRules.fitsWithin(30 + 40 + 20 + 10.01, perDay));
        // 500 over a 5-day trip would fit the trip total but not the per-day amount.
        assertFalse(AmountRules.fitsWithin(500, perDay));
    }
}
