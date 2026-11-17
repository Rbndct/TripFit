package com.itismob.s01.group01.tripfit.prototype;

import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Pure validation and formatting for money fields: plain decimal text only (no signs, exponents
 * or "Infinity"), at most {@link #MAX_AMOUNT}, rounded to two decimals. Out-of-range input is
 * rejected, never clamped.
 */
public final class AmountRules {

    public static final double MAX_AMOUNT = 100_000_000;
    public static final double MIN_EXPENSE = 0.01;

    private static final String PLAIN_DECIMAL = "\\d*\\.?\\d*";

    private AmountRules() { }

    /** Amount in {@code min..MAX_AMOUNT}, or null when blank, malformed or out of range. */
    @Nullable
    public static Double parse(@Nullable String text, double min) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.matches(PLAIN_DECIMAL) || !trimmed.matches(".*\\d.*")) {
            return null;
        }
        // Rounded from the typed digits, so "1.005" is 1.01 (a double would hold 1.00499...).
        BigDecimal value = new BigDecimal(trimmed).setScale(2, RoundingMode.HALF_UP);
        boolean inRange = value.compareTo(BigDecimal.valueOf(min)) >= 0
                && value.compareTo(BigDecimal.valueOf(MAX_AMOUNT)) <= 0;
        return inRange ? value.doubleValue() : null;
    }

    /** An expense amount: 0.01 to 100,000,000. */
    @Nullable
    public static Double parseExpense(@Nullable String text) {
        return parse(text, MIN_EXPENSE);
    }

    /** A strictly positive budget amount: 0.01 to 100,000,000. */
    @Nullable
    public static Double parseBudget(@Nullable String text) {
        return parse(text, MIN_EXPENSE);
    }

    /** A stop cost or category limit: blank means 0, otherwise 0 to 100,000,000; null if invalid. */
    @Nullable
    public static Double parseOptional(@Nullable String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        return parse(text, 0);
    }

    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Editable text with exactly two decimals and no grouping, e.g. "4500.00". */
    public static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    /** True when {@code sum <= limit} compared in whole cents, so float noise never rejects. */
    public static boolean fitsWithin(double sum, double limit) {
        return Math.round(sum * 100.0) <= Math.round(limit * 100.0);
    }
}
