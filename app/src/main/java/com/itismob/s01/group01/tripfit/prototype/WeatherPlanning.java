package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;

import java.util.ArrayList;
import java.util.List;

/** Shared sample forecasts and outdoor-stop rules for the itinerary and Weather screen. */
public final class WeatherPlanning {
    public enum Condition {
        CLEAR("Clear", 26, 10),
        RAIN("Rain", 22, 80),
        STORM("Storm", 20, 95);

        final String storedLabel;
        public final int temperatureC;
        public final int precipitationChance;

        Condition(String storedLabel, int temperatureC, int precipitationChance) {
            this.storedLabel = storedLabel;
            this.temperatureC = temperatureC;
            this.precipitationChance = precipitationChance;
        }
    }

    private WeatherPlanning() { }

    public static Condition conditionAt(List<String> saved, int day) {
        if (day >= 1 && day <= saved.size()) {
            String label = saved.get(day - 1);
            for (Condition condition : Condition.values()) {
                if (condition.storedLabel.equals(label)) {
                    return condition;
                }
            }
            if ("Sunny".equals(label)) {
                return Condition.CLEAR;
            }
        }
        return day % 3 == 2 ? Condition.RAIN : Condition.CLEAR;
    }

    public static boolean isOutdoor(ItineraryItemWithPlace stop, PrototypeStore store) {
        ItemDetails details = store.itemDetails(stop.item.getId());
        if (details != null) {
            return details.isOutdoor();
        }
        DemoCatalog.Place place = DemoCatalog.findById(stop.item.getPlaceId());
        if (place == null) {
            // Older sample stops were originally stored as names without linked place IDs.
            place = DemoCatalog.findByName(stop.item.displayName(stop.placeName));
        }
        return place == null || place.outdoor;
    }

    public static List<ItineraryItemWithPlace> conflicts(List<ItineraryItemWithPlace> itinerary,
                                                        int day, Condition condition,
                                                        PrototypeStore store) {
        List<ItineraryItemWithPlace> conflicts = new ArrayList<>();
        if (condition != Condition.CLEAR) {
            for (ItineraryItemWithPlace stop : itinerary) {
                if (stop.item.getDay() == day && isOutdoor(stop, store)) {
                    conflicts.add(stop);
                }
            }
        }
        return conflicts;
    }

    public static boolean canTradeDays(ItineraryItemWithPlace outdoor,
                                       ItineraryItemWithPlace indoor, PrototypeStore store) {
        return !outdoor.item.isBooked() && !indoor.item.isBooked()
                && outdoor.item.getDay() != indoor.item.getDay()
                && isOutdoor(outdoor, store) && !isOutdoor(indoor, store);
    }
}
