package com.itismob.s01.group01.tripfit.data;

import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Small bundled city/place set used when Supabase is unreachable, so search still demos offline. */
final class OfflineData {

    static final String TOKYO_ID = "offline-tokyo";
    static final String MANILA_ID = "offline-manila";

    private static final List<CityDto> CITIES = Collections.unmodifiableList(Arrays.asList(
            new CityDto(TOKYO_ID, "Tokyo", "Japan", 35.6762, 139.6503),
            new CityDto(MANILA_ID, "Manila", "Philippines", 14.5995, 120.9842)));

    private static final List<PlaceDto> PLACES = Collections.unmodifiableList(Arrays.asList(
            place("offline-senso-ji", TOKYO_ID, "Senso-ji Temple", "temple", 35.7148, 139.7967, false),
            place("offline-skytree", TOKYO_ID, "Tokyo Skytree", "landmark", 35.7101, 139.8107, false),
            place("offline-meiji", TOKYO_ID, "Meiji Shrine", "shrine", 35.6764, 139.6993, false),
            place("offline-shibuya", TOKYO_ID, "Shibuya Crossing", "landmark", 35.6595, 139.7005, false),
            place("offline-nezu", TOKYO_ID, "Nezu Shrine", "shrine", 35.7267, 139.7606, true),
            place("offline-teamlab", TOKYO_ID, "teamLab Planets", "museum", 35.6490, 139.7893, false),
            place("offline-intramuros", MANILA_ID, "Intramuros", "historic", 14.5896, 120.9747, false),
            place("offline-rizal", MANILA_ID, "Rizal Park", "park", 14.5826, 120.9787, false),
            place("offline-binondo", MANILA_ID, "Binondo Food Crawl", "food", 14.6001, 120.9742, true),
            place("offline-fort", MANILA_ID, "Fort Santiago", "historic", 14.5953, 120.9660, false)));

    private OfflineData() { }

    static List<CityDto> cities() {
        return CITIES;
    }

    /** Case-insensitive name-substring search within one city, mirroring the remote query. */
    static List<PlaceDto> search(String cityId, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<PlaceDto> matches = new ArrayList<>();
        for (PlaceDto place : PLACES) {
            if (place.getCityId().equals(cityId)
                    && place.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(place);
            }
        }
        return matches;
    }

    private static PlaceDto place(String id, String cityId, String name, String category,
                                  double lat, double lng, boolean hiddenGem) {
        return new PlaceDto(id, cityId, name, category, lat, lng, null, null, 4.4, hiddenGem,
                null, null);
    }
}
