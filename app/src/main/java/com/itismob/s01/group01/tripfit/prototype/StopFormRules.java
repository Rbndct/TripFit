package com.itismob.s01.group01.tripfit.prototype;

import androidx.annotation.Nullable;

/** Pure validation for the stop forms. Out-of-range input is rejected, never clamped. */
public final class StopFormRules {

    public static final int MIN_DURATION_MIN = 5;
    public static final int MAX_DURATION_MIN = 720;
    public static final int MIN_BUFFER_MIN = 0;
    public static final int MAX_BUFFER_MIN = 180;

    private StopFormRules() { }

    /** Blank is valid (it means the default stay); otherwise 5..720 whole minutes. */
    public static boolean isDurationValid(@Nullable String text) {
        if (isBlank(text)) {
            return true;
        }
        Integer value = parse(text);
        return value != null && value >= MIN_DURATION_MIN && value <= MAX_DURATION_MIN;
    }

    /** The typed duration, or {@link ScheduleMath#DEFAULT_STAY_MIN} when blank. Call after validating. */
    public static int durationOrDefault(@Nullable String text) {
        Integer value = isBlank(text) ? null : parse(text);
        return value != null ? value : ScheduleMath.DEFAULT_STAY_MIN;
    }

    /** Blank is valid (profile default); otherwise 0..180 whole minutes. */
    public static boolean isBufferValid(@Nullable String text) {
        if (isBlank(text)) {
            return true;
        }
        Integer value = parse(text);
        return value != null && value >= MIN_BUFFER_MIN && value <= MAX_BUFFER_MIN;
    }

    /** The typed buffer, or null when blank (use the profile default). Zero is kept. */
    @Nullable
    public static Integer bufferOrNull(@Nullable String text) {
        return isBlank(text) ? null : parse(text);
    }

    /** Text for the buffer field: blank when no explicit buffer is stored, so saving keeps null. */
    public static String bufferFieldText(@Nullable Integer stored) {
        return stored == null ? "" : String.valueOf(stored);
    }

    /** A required whole day in 1..tripDays. */
    public static boolean isDayValid(@Nullable String text, int tripDays) {
        Integer value = isBlank(text) ? null : parse(text);
        return value != null && value >= 1 && value <= tripDays;
    }

    public static boolean isPinValid(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private static boolean isBlank(@Nullable String text) {
        return text == null || text.trim().isEmpty();
    }

    @Nullable
    private static Integer parse(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
