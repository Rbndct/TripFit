package com.itismob.s01.group01.tripfit.ui;

import java.util.Locale;

/** Safe cache-file names for shared trip exports. */
public final class ExportNames {
    private ExportNames() { }

    public static String safeName(String title) {
        String cleaned = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (cleaned.length() > 120) {
            cleaned = cleaned.substring(0, 120).replaceAll("-$", "");
        }
        return cleaned.isEmpty() ? "trippings-itinerary" : "trippings-" + cleaned;
    }
}
