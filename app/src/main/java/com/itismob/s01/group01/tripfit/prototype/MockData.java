package com.itismob.s01.group01.tripfit.prototype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Hard-coded demo content standing in for Foursquare, Wikivoyage, OpenWeather and distance APIs. */
public final class MockData {

    public static final List<String> CITIES = Collections.unmodifiableList(
            Arrays.asList("Tokyo", "Manila", "Seoul", "Singapore", "Paris", "New York"));

    private static final String[] OUTDOOR_KEYWORDS =
            {"temple", "shrine", "crossing", "garden", "park", "skytree", "street", "walk", "cruise"};

    private MockData() { }

    public static List<MockPlace> famous(String city) {
        String catalogCity = DemoCatalog.city(city) == null ? DemoCatalog.City.TOKYO.name : city;
        return catalogPlaces(catalogCity, CatalogSuggestions.Filter.FAMOUS, false);
    }

    private static List<MockPlace> catalogPlaces(String city, CatalogSuggestions.Filter filter,
                                                  boolean indoorOnly) {
        List<MockPlace> places = new ArrayList<>();
        for (DemoCatalog.Place place : DemoCatalog.places(city)) {
            if (filter.matches(place) && (!indoorOnly || !place.outdoor)) {
                places.add(new MockPlace(place.name, place.category, place.rating, place.reviews,
                        0, place.price, !place.outdoor));
            }
        }
        return places;
    }

    /** Local currency of a demo city; unknown cities default to USD. */
    public static String currencyFor(String city) {
        DemoCatalog.City sample = DemoCatalog.city(city);
        if (sample != null) {
            return sample.currency;
        }
        switch (city) {
            case "Seoul":
                return "KRW";
            case "Singapore":
                return "SGD";
            case "Paris":
                return "EUR";
            default:
                return "USD";
        }
    }

    /** Demo forecast: Day 2 is rainy so the weather-aware flow always has something to flag. */
    public static boolean isRainy(int day) {
        return day == 2;
    }

    public static boolean isOutdoor(String stopName) {
        String lower = stopName.toLowerCase(Locale.ROOT);
        for (String keyword : OUTDOOR_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /** Indoor stops offered as rain alternatives. */
    public static List<MockPlace> indoorAlternatives() {
        return catalogPlaces("Tokyo", CatalogSuggestions.Filter.ACTIVITIES, true);
    }
}
