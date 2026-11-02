package com.itismob.s01.group01.tripfit.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Reverse-geocodes a single map-picked coordinate via Nominatim. This is one request per
 * user confirm tap -- not bulk -- so it's fine to call directly from the app per Nominatim's
 * usage policy (docs/api-terms.md); batched/heavy geocoding still only happens in the
 * ingestion pipeline, never per-device.
 */
public class NominatimReverseGeocoder {

    private static final String USER_AGENT =
            "TripFit-Android/1.0 (DLSU student project; contact rbenedict.maagma@gmail.com)";
    private static final String REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /** Blocking call -- invoke from a background thread. Returns a short place name, or null. */
    public String reverseGeocode(double lat, double lng) {
        String url = String.format(Locale.US, "%s?lat=%f&lon=%f&format=jsonv2&zoom=18",
                REVERSE_URL, lat, lng);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            JSONObject json = new JSONObject(response.body().string());
            String name = json.optString("name", "");
            if (!name.isEmpty()) {
                return name;
            }
            String displayName = json.optString("display_name", "");
            return displayName.isEmpty() ? null : displayName.split(",")[0].trim();
        } catch (IOException | JSONException e) {
            return null;
        }
    }
}
