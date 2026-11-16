package com.itismob.s01.group01.tripfit.prototype;

import androidx.annotation.Nullable;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/** Pure validation rules for the trip form. Out-of-range input is rejected, never clamped. */
public final class TripFormRules {

    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 30;
    public static final double MIN_BUDGET = 1;
    public static final double MAX_BUDGET = 100_000_000;

    private TripFormRules() { }

    /** True only for a real calendar date written exactly as {@code yyyy-MM-dd}. */
    public static boolean isValidIsoDate(@Nullable String text) {
        if (text == null || !text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        ParsePosition position = new ParsePosition(0);
        return format.parse(text, position) != null && position.getIndex() == text.length();
    }

    /** Whole number of days in 1..30, or null when blank, not a whole number or out of range. */
    @Nullable
    public static Integer parseDays(@Nullable String text) {
        if (text == null) {
            return null;
        }
        try {
            int days = Integer.parseInt(text.trim());
            return days >= MIN_DAYS && days <= MAX_DAYS ? days : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Budget in 1..100,000,000, or null when blank, non-finite or out of range. */
    @Nullable
    public static Double parseBudget(@Nullable String text) {
        if (text == null || !text.trim().matches("\\d+(\\.\\d+)?")) {
            return null;
        }
        try {
            double amount = Double.parseDouble(text.trim());
            boolean ok = !Double.isNaN(amount) && !Double.isInfinite(amount)
                    && amount >= MIN_BUDGET && amount <= MAX_BUDGET;
            return ok ? amount : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A trip may only get shorter than its last stop's day when no stop would be orphaned. */
    public static boolean canShorten(int newDays, int maxStopDay) {
        return maxStopDay <= newDays;
    }
}
