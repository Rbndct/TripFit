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

    private static final List<MockPlace> FOOD = Collections.unmodifiableList(Arrays.asList(
            new MockPlace("Afuri Ramen", "Ramen", 4.3, 2100, 0.4, 1200, true),
            new MockPlace("Ichiran Shibuya", "Ramen", 4.4, 5400, 0.9, 1500, true),
            new MockPlace("Gonpachi Nishiazabu", "Izakaya", 4.2, 3800, 2.1, 3800, true),
            new MockPlace("Tsukiji Sushi Zanmai", "Sushi", 4.5, 6200, 3.4, 4500, true),
            new MockPlace("Kaikaya", "Seafood", 4.6, 1400, 1.2, 6000, true),
            new MockPlace("Daiwa Sushi", "Sushi", 4.7, 2900, 3.5, 9000, true)));

    private static final List<MockPlace> ACTIVITIES = Collections.unmodifiableList(Arrays.asList(
            new MockPlace("Meiji Jingu Garden", "Garden", 4.5, 8800, 0.8, 500, false),
            new MockPlace("Tokyo National Museum", "Museum", 4.4, 9600, 2.6, 1000, true),
            new MockPlace("Sumida River Cruise", "Tour", 4.2, 3100, 1.7, 1720, false),
            new MockPlace("teamLab Planets", "Experience", 4.6, 21000, 4.8, 3800, true),
            new MockPlace("Shibuya Sky", "Viewpoint", 4.6, 18500, 0.5, 2200, false),
            new MockPlace("Tokyo Skytree Tembo Deck", "Viewpoint", 4.4, 26000, 5.2, 2100, true)));

    private static final List<MockPlace> TOKYO_FAMOUS = Collections.unmodifiableList(Arrays.asList(
            new MockPlace("Senso-ji Temple", "Temple", 4.6, 62000, 0, 0, false),
            new MockPlace("Shibuya Crossing", "Landmark", 4.5, 48000, 0, 0, false),
            new MockPlace("Tokyo Skytree", "Landmark", 4.4, 26000, 0, 2100, true),
            new MockPlace("Meiji Shrine", "Shrine", 4.6, 38000, 0, 0, false)));

    private static final List<MockPlace> TOKYO_GEMS = Collections.unmodifiableList(Arrays.asList(
            new MockPlace("Nezu Shrine", "Shrine", 4.5, 2400, 0, 0, false),
            new MockPlace("Yanaka Ginza", "Old town street", 4.4, 1900, 0, 0, false),
            new MockPlace("Todoroki Valley", "Park", 4.5, 900, 0, 0, false),
            new MockPlace("Shimokitazawa", "Neighbourhood", 4.3, 1500, 0, 0, true)));

    private static final List<MockPlace> MANILA_FAMOUS = Collections.unmodifiableList(Arrays.asList(
            new MockPlace("Intramuros", "Historic district", 4.5, 31000, 0, 0, false),
            new MockPlace("Rizal Park", "Park", 4.5, 28000, 0, 0, false),
            new MockPlace("National Museum of Fine Arts", "Museum", 4.6, 7200, 0, 0, true),
            new MockPlace("Manila Cathedral", "Church", 4.6, 9400, 0, 0, true)));

    private static final List<MockPlace> MANILA_GEMS = Collections.unmodifiableList(Arrays.asList(
            new MockPlace("Binondo Food Crawl", "Food street", 4.6, 1300, 0, 0, false),
            new MockPlace("San Agustin Church Museum", "Museum", 4.4, 1100, 0, 0, true),
            new MockPlace("Manila Bay Sunset Walk", "Waterfront", 4.2, 800, 0, 0, false),
            new MockPlace("Escolta Street", "Heritage street", 4.3, 700, 0, 0, false)));

    private static final String[] OUTDOOR_KEYWORDS =
            {"temple", "shrine", "crossing", "garden", "park", "skytree", "street", "walk", "cruise"};

    private MockData() { }

    public static List<MockPlace> food() {
        return FOOD;
    }

    public static List<MockPlace> activities() {
        return ACTIVITIES;
    }

    public static List<MockPlace> famous(String city) {
        return "Manila".equals(city) ? MANILA_FAMOUS : TOKYO_FAMOUS;
    }

    public static List<MockPlace> hiddenGems(String city) {
        return "Manila".equals(city) ? MANILA_GEMS : TOKYO_GEMS;
    }

    /** Local currency of a demo city; unknown cities default to USD. */
    public static String currencyFor(String city) {
        switch (city) {
            case "Tokyo":
                return "JPY";
            case "Manila":
                return "PHP";
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
        List<MockPlace> indoor = new ArrayList<>();
        for (MockPlace place : ACTIVITIES) {
            if (place.isIndoor()) {
                indoor.add(place);
            }
        }
        return indoor;
    }
}
