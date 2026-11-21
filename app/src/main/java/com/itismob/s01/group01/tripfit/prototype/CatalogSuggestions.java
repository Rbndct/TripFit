package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CatalogSuggestions {
    public enum Filter {
        ALL, FAMOUS, HIDDEN, FOOD, ACTIVITIES;

        public boolean matches(DemoCatalog.Place place) {
            switch (this) {
                case FAMOUS: return !place.hidden && !FOOD.matches(place);
                case HIDDEN: return place.hidden;
                case FOOD: return "Food".equals(place.category);
                case ACTIVITIES: return "Activities".equals(place.category);
                default: return true;
            }
        }
    }

    public static Set<String> plannedNames(List<ItineraryItemWithPlace> itinerary) {
        Set<String> names = new HashSet<>();
        for (ItineraryItemWithPlace entry : itinerary) {
            names.add(DemoCatalog.nameKey(entry.item.displayName(entry.placeName)));
        }
        return names;
    }

    public static List<DemoCatalog.Place> suggested(String city, Set<String> planned) {
        List<DemoCatalog.Place> famous = new ArrayList<>();
        List<DemoCatalog.Place> hidden = new ArrayList<>();
        for (DemoCatalog.Place place : DemoCatalog.places(city)) {
            if (Filter.FOOD.matches(place) || planned.contains(DemoCatalog.nameKey(place.name))) {
                continue;
            }
            List<DemoCatalog.Place> group = place.hidden ? hidden : famous;
            if (group.size() < (place.hidden ? 2 : 3)) {
                group.add(place);
            }
        }
        famous.addAll(hidden);
        return famous;
    }

    public static int dayForPick(int day, int tripDays, int index, boolean spread) {
        return Math.min(tripDays, day + (spread ? index / 3 : 0));
    }

    public static double remaining(double limit, double planned, double logged) {
        return limit - Math.max(planned, logged);
    }

    /**
     * What a suggestion may still cost: the tighter of the category and whole-trip remaining.
     * A limit of 0 means "no limit" and is ignored; with no limits at all the result is unbounded.
     */
    public static double allowance(double categoryLimit, double categoryPlanned, double categoryLogged,
                                   double tripLimit, double tripPlanned, double tripLogged) {
        double allowance = Double.POSITIVE_INFINITY;
        if (categoryLimit > 0) {
            allowance = remaining(categoryLimit, categoryPlanned, categoryLogged);
        }
        if (tripLimit > 0) {
            allowance = Math.min(allowance, remaining(tripLimit, tripPlanned, tripLogged));
        }
        return allowance;
    }

    public static final class Anchor {
        public final String name;
        private final double lat, lng;

        private Anchor(String name, double lat, double lng) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
        }

        public double distance(DemoCatalog.Place place) {
            return RoutePreview.distance(lat, lng, place.lat, place.lng);
        }
    }

    public static Anchor anchor(String city, int day, List<ItineraryItemWithPlace> itinerary) {
        ItineraryItemWithPlace last = null;
        for (ItineraryItemWithPlace entry : itinerary) {
            if (entry.item.getDay() == day && entry.placeLat != null && entry.placeLng != null
                    && StopFormRules.isPinValid(entry.placeLat, entry.placeLng)
                    && (last == null || entry.item.getPosition() >= last.item.getPosition())) {
                last = entry;
            }
        }
        return last == null ? new Anchor(null, DemoCatalog.lat(city), DemoCatalog.lng(city))
                : new Anchor(last.item.displayName(last.placeName), last.placeLat, last.placeLng);
    }

    public static List<DemoCatalog.Place> nearby(String city, String query, Filter filter,
                                                  Anchor anchor) {
        List<DemoCatalog.Place> places = DemoCatalog.search(city, query);
        places.removeIf(place -> !filter.matches(place));
        places.sort(Comparator.comparingDouble(anchor::distance).thenComparing(place -> place.name));
        return places;
    }

    private CatalogSuggestions() { }
}
