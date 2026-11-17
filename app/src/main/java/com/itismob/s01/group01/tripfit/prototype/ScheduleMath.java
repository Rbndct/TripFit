package com.itismob.s01.group01.tripfit.prototype;

import java.util.Locale;

/** Clock helpers and the day-planning constants shared by {@link RoutePreview}. */
public final class ScheduleMath {

    public static final int DAY_START_MIN = 9 * 60;
    public static final int DEFAULT_STAY_MIN = 60;
    /** A flexible stop ending after this minute of the day (20:00) is flagged as late. */
    public static final int LATE_END_MIN = 20 * 60;
    /** End of the day (24:00); a booking running past it is flagged. */
    public static final int DAY_END_MIN = 24 * 60;
    /** Assumed leg length when either end of a leg has no pin. */
    public static final int PLACEHOLDER_TRAVEL_MIN = 20;
    /** Buffer before an incoming leg when neither the stop nor the profile sets one. */
    public static final int DEFAULT_BUFFER_MIN = 15;

    private ScheduleMath() { }

    public static int parse(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public static String format(int minuteOfDay) {
        int wrapped = minuteOfDay % (24 * 60);
        return String.format(Locale.US, "%02d:%02d", wrapped / 60, wrapped % 60);
    }
}
