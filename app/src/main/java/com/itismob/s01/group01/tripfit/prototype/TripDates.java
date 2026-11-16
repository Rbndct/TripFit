package com.itismob.s01.group01.tripfit.prototype;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Date helpers that turn the ISO start date plus trip length into the display label. */
public final class TripDates {

    private static final String ISO = "yyyy-MM-dd";
    private static final String SHORT = "MMM d";

    private TripDates() { }

    /** "Dec 1 – Dec 3" for a start date and length; "" when the start date is not valid ISO. */
    public static String label(String startIso, int days) {
        if (!TripFormRules.isValidIsoDate(startIso)) {
            return "";
        }
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        start.setTimeInMillis(parseIso(startIso).getTime());
        SimpleDateFormat format = utc(SHORT);
        String first = format.format(start.getTime());
        start.add(Calendar.DAY_OF_MONTH, Math.max(1, days) - 1);
        String last = format.format(start.getTime());
        return days <= 1 ? first : first + " – " + last;
    }

    /** The UTC calendar day of a date-picker timestamp as {@code yyyy-MM-dd}. */
    public static String isoFromUtcMillis(long millis) {
        return utc(ISO).format(new Date(millis));
    }

    /** Number of days from the first to the last timestamp, counting both ends. */
    public static int daysBetweenInclusive(long firstMillis, long lastMillis) {
        return (int) TimeUnit.MILLISECONDS.toDays(lastMillis - firstMillis) + 1;
    }

    /** Today in the device time zone as {@code yyyy-MM-dd}. */
    public static String todayIso() {
        return new SimpleDateFormat(ISO, Locale.US).format(new Date());
    }

    private static Date parseIso(String iso) {
        try {
            return utc(ISO).parse(iso);
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException(iso, e);
        }
    }

    private static SimpleDateFormat utc(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}
