package com.itismob.s01.group01.tripfit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BudgetLinesTest {

    private static final String REMAINING = "%1$s remaining of %2$s";
    private static final String OVER = "Over budget by %1$s of %2$s";
    private static final String NO_LIMIT = "%1$s spent, no limit";

    private static String line(double spent, double limit) {
        return BudgetLines.categoryLine(spent, limit, "PHP", REMAINING, OVER, NO_LIMIT);
    }

    @Test
    public void underLimit_readsRemainingOf() {
        assertEquals("₱1,000.00 remaining of ₱4,000.00", line(3000, 4000));
    }

    @Test
    public void exactlyAtLimit_isZeroRemaining_notOver() {
        assertEquals("₱0.00 remaining of ₱4,000.00", line(4000, 4000));
    }

    @Test
    public void overLimit_containsOverBudget() {
        String text = line(4293, 4000);
        assertTrue(text, text.contains("Over budget"));
        assertEquals("Over budget by ₱293.00 of ₱4,000.00", text);
    }

    @Test
    public void noLimit_usesNoLimitLine() {
        assertEquals("₱25.00 spent, no limit", line(25, 0));
    }

    @Test
    public void nothingSpent_remainingIsWholeLimit() {
        assertEquals("₱500.00 remaining of ₱500.00", line(0, 500));
    }
}
