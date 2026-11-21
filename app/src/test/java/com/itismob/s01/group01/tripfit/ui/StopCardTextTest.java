package com.itismob.s01.group01.tripfit.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StopCardTextTest {

    @Test
    public void summary_joinsCategoryDurationAndEstimate() {
        assertEquals("Food · 45 min · Est. ₱56.00",
                StopCardText.summary("Food", "45 min", "Est. ₱56.00"));
    }

    @Test
    public void summary_skipsMissingEstimate() {
        assertEquals("Activities · 60 min", StopCardText.summary("Activities", "60 min", null));
        assertEquals("Activities · 60 min", StopCardText.summary("Activities", "60 min", ""));
    }

    @Test
    public void summary_skipsBlankParts() {
        assertEquals("45 min", StopCardText.summary("  ", "45 min", null));
    }

    @Test
    public void summary_allBlank_isEmpty() {
        assertEquals("", StopCardText.summary(null, null, null));
    }

    @Test
    public void summary_isUnicodeSafe() {
        assertEquals("Café ☕ · 30 min · ¥1,000",
                StopCardText.summary("Café ☕", "30 min", "¥1,000"));
    }
}
