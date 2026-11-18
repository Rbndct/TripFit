package com.itismob.s01.group01.tripfit.prototype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Hard-coded demo content standing in for Foursquare, Wikivoyage, OpenWeather and distance APIs. */
public final class MockData {

    public static final List<String> CITIES = Collections.unmodifiableList(
            Arrays.asList("Tokyo", "Manila", "Seoul", "Singapore", "Paris", "New York"));

    private MockData() { }

    public static List<MockPlace> famous(String city) {
        String catalogCity = DemoCatalog.city(city) == null ? DemoCatalog.City.TOKYO.name : city;
        return catalogPlaces(catalogCity);
    }

    private static List<MockPlace> catalogPlaces(String city) {
        List<MockPlace> places = new ArrayList<>();
        for (DemoCatalog.Place place : DemoCatalog.places(city)) {
            if (CatalogSuggestions.Filter.FAMOUS.matches(place)) {
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

}
