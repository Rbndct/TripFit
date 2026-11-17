package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StopFormRulesTest {

    @Test
    public void duration_blankIsValidAndMeansDefaultStay() {
        assertTrue(StopFormRules.isDurationValid(""));
        assertTrue(StopFormRules.isDurationValid("  "));
        assertEquals(ScheduleMath.DEFAULT_STAY_MIN, StopFormRules.durationOrDefault(""));
    }

    @Test
    public void duration_acceptsFiveToSevenTwentyInclusive() {
        assertTrue(StopFormRules.isDurationValid("5"));
        assertTrue(StopFormRules.isDurationValid("720"));
        assertEquals(720, StopFormRules.durationOrDefault("720"));
    }

    @Test
    public void duration_rejectsOutOfRangeAndNonNumbers() {
        assertFalse(StopFormRules.isDurationValid("4"));
        assertFalse(StopFormRules.isDurationValid("721"));
        assertFalse(StopFormRules.isDurationValid("0"));
        assertFalse(StopFormRules.isDurationValid("abc"));
        assertFalse(StopFormRules.isDurationValid("99999999999"));
    }

    @Test
    public void buffer_blankMeansProfileDefaultAndZeroIsKept() {
        assertTrue(StopFormRules.isBufferValid(""));
        assertNull(StopFormRules.bufferOrNull(""));
        assertEquals(Integer.valueOf(0), StopFormRules.bufferOrNull("0"));
        assertEquals(Integer.valueOf(180), StopFormRules.bufferOrNull("180"));
    }

    @Test
    public void buffer_rejectsAboveOneEightyAndNonNumbers() {
        assertFalse(StopFormRules.isBufferValid("181"));
        assertFalse(StopFormRules.isBufferValid("-1"));
        assertFalse(StopFormRules.isBufferValid("x"));
    }

    @Test
    public void day_mustBeBetweenOneAndTripDays() {
        assertTrue(StopFormRules.isDayValid("1", 3));
        assertTrue(StopFormRules.isDayValid("3", 3));
        assertFalse(StopFormRules.isDayValid("0", 3));
        assertFalse(StopFormRules.isDayValid("4", 3));
        assertFalse(StopFormRules.isDayValid("", 3));
        assertFalse(StopFormRules.isDayValid("x", 3));
    }

    @Test
    public void pin_latitudeAndLongitudeRangesAreInclusive() {
        assertTrue(StopFormRules.isPinValid(90, 180));
        assertTrue(StopFormRules.isPinValid(-90, -180));
        assertFalse(StopFormRules.isPinValid(90.01, 0));
        assertFalse(StopFormRules.isPinValid(0, 180.01));
        assertFalse(StopFormRules.isPinValid(Double.NaN, 0));
    }

    @Test
    public void bufferField_untouchedDefaultStaysBlankAndSavesNull() {
        String field = StopFormRules.bufferFieldText(null);
        assertEquals("", field);
        assertNull(StopFormRules.bufferOrNull(field));
    }

    @Test
    public void bufferField_explicitZeroStaysZero() {
        String field = StopFormRules.bufferFieldText(0);
        assertEquals("0", field);
        assertEquals(Integer.valueOf(0), StopFormRules.bufferOrNull(field));
        assertEquals(Integer.valueOf(25), StopFormRules.bufferOrNull(StopFormRules.bufferFieldText(25)));
    }

    @Test
    public void pin_unsetNaNDefaultIsRejected() {
        assertFalse(StopFormRules.isPinValid(Double.NaN, Double.NaN));
        assertFalse(StopFormRules.isPinValid(10, Double.NaN));
    }
}
