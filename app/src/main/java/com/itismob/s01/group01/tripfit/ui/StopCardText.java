package com.itismob.s01.group01.tripfit.ui;

import java.util.ArrayList;
import java.util.List;

/** Pure text composition for the itinerary stop card's summary line. */
public final class StopCardText {

    private static final String SEPARATOR = " · ";

    private StopCardText() { }

    /** "category · duration · estimate", leaving out any part that is null or blank. */
    public static String summary(String category, String duration, String estimate) {
        List<String> parts = new ArrayList<>();
        for (String part : new String[]{category, duration, estimate}) {
            if (part != null && !part.trim().isEmpty()) {
                parts.add(part.trim());
            }
        }
        return String.join(SEPARATOR, parts);
    }
}
