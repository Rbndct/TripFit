package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Adapter between Room rows plus the persistent store and the pure {@link TripSnapshot} snapshot the
 * {@link RoutePreview} engine plans on. It also owns the mapping between the engine's display
 * mode names (Walking, Cycling, Driving, Transit, Suggested) and {@link TravelMode}.
 */
public final class TripSnapshots {

    static final String SUGGESTED = "Suggested";

    private TripSnapshots() { }

    /** One stop's new day and position after balancing. */
    public static final class ItemMove {
        public final String id;
        public final int day;
        public final int position;

        public ItemMove(String id, int day, int position) {
            this.id = id;
            this.day = day;
            this.position = position;
        }
    }

    /** Engine name for a stored mode; null means Suggested. */
    public static String modeName(TravelMode mode) {
        if (mode == null) {
            return SUGGESTED;
        }
        switch (mode) {
            case WALK: return "Walking";
            case CYCLE: return "Cycling";
            case DRIVE: return "Driving";
            default: return "Transit";
        }
    }

    /** Stored mode for an engine name; null for "Suggested" or anything unknown. */
    public static TravelMode toMode(String engineName) {
        if (engineName == null) {
            return null;
        }
        switch (engineName) {
            case "Walking": return TravelMode.WALK;
            case "Cycling": return TravelMode.CYCLE;
            case "Driving": return TravelMode.DRIVE;
            case "Transit": return TravelMode.TRANSIT;
            default: return null;
        }
    }

    /** Builds the planning snapshot for a trip from its rows; the store supplies defaults and extras. */
    public static TripSnapshot from(String tripId, TripMeta meta, List<ItineraryItemWithPlace> items,
                                 PrototypeStore store) {
        TripSnapshot trip = new TripSnapshot();
        trip.id = tripId;
        int maxDay = 0;
        for (ItineraryItemWithPlace entry : items) {
            maxDay = Math.max(maxDay, entry.item.getDay());
            trip.stops.add(stopFrom(entry, store));
        }
        trip.days = Math.max(meta.getDays(), maxDay);
        trip.city = meta.getCity();
        trip.homeCurrency = meta.getHomeCurrency();
        return trip;
    }

    /** Adds persisted dates, budget and original expense currencies to the route snapshot. */
    public static TripSnapshot forExport(String tripId, String name, List<ItineraryItemWithPlace> items,
                                      PrototypeStore store) {
        TripMeta meta = store.meta(tripId);
        TripSnapshot trip = from(tripId, meta, items, store);
        trip.name = name;
        trip.startDate = meta.getStartDateIso();
        BudgetSettings budget = store.budget(tripId);
        // Budget and Suggestions use configured trip length for the limits, even on older trips
        // with a stop beyond that length. Export those same whole-trip amounts.
        trip.budget = budget.tripTotal(meta.getDays());
        trip.foodBudget = budget.limitFor(Category.FOOD, meta.getDays());
        trip.activityBudget = budget.limitFor(Category.ACTIVITIES, meta.getDays());
        trip.transportBudget = budget.limitFor(Category.TRANSPORT, meta.getDays());
        trip.otherBudget = budget.limitFor(Category.OTHER, meta.getDays());
        for (Expense expense : store.expenses(tripId)) {
            TripSnapshot.Expense saved = new TripSnapshot.Expense();
            saved.id = expense.getId();
            saved.title = expense.getTitle();
            saved.category = BudgetMath.categoryName(expense.getCategory());
            saved.currency = expense.getLocalCurrency();
            saved.amount = expense.getAmountLocal();
            trip.expenses.add(saved);
        }
        return trip;
    }

    private static TripSnapshot.Stop stopFrom(ItineraryItemWithPlace entry, PrototypeStore store) {
        ItineraryItemEntity item = entry.item;
        TripSnapshot.Stop stop = new TripSnapshot.Stop();
        stop.id = item.getId();
        stop.name = item.displayName(entry.placeName);
        stop.address = entry.placeAddress == null ? "" : entry.placeAddress;
        stop.day = item.getDay();
        stop.duration = item.getDurationMin() != null
                ? item.getDurationMin() : ScheduleMath.DEFAULT_STAY_MIN;
        stop.explicitBuffer = item.getBufferMin();
        stop.buffer = item.getBufferMin() != null ? item.getBufferMin() : store.getBufferMin();
        stop.mode = modeName(TripDocMapper.parseMode(item.getTransportMode()));
        stop.hasLocation = entry.placeLat != null && entry.placeLng != null;
        if (stop.hasLocation) {
            stop.lat = entry.placeLat;
            stop.lng = entry.placeLng;
        }
        stop.booked = item.isBooked();
        if (stop.booked) {
            stop.fixedMinute = parseOr(item.getTime(), ScheduleMath.DAY_START_MIN);
        }
        applyDetails(stop, store.itemDetails(item.getId()));
        return stop;
    }

    private static void applyDetails(TripSnapshot.Stop stop, ItemDetails details) {
        if (details == null) {
            return;
        }
        stop.cost = details.getCost();
        if (details.getCostCurrency() != null) {
            stop.currency = details.getCostCurrency();
        }
        if (details.getCategory() != null) {
            stop.category = details.getCategory();
        }
        stop.outdoor = details.isOutdoor();
    }

    private static int parseOr(String hhmm, int fallback) {
        if (hhmm == null) {
            return fallback;
        }
        try {
            return ScheduleMath.parse(hhmm);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Day number to schedule/show: {@code selected} limited to 1..days (days below 1 count as 1). */
    public static int clampDay(int selected, int days) {
        return Math.max(1, Math.min(selected, Math.max(1, days)));
    }

    /**
     * Compatibility for Calendar Sync and Summary: each day's current-order schedule, including
     * travel and per-stop buffer. Days without stops are omitted.
     */
    public static Map<Integer, List<RoutePreview.Slot>> schedule(
            String tripId, TripMeta meta, List<ItineraryItemWithPlace> items, PrototypeStore store) {
        TripSnapshot trip = from(tripId, meta, items, store);
        Map<Integer, List<RoutePreview.Slot>> byDay = new TreeMap<>();
        for (int day = 1; day <= trip.days; day++) {
            List<RoutePreview.Slot> slots = RoutePreview.day(trip, day, false);
            if (!slots.isEmpty()) {
                byDay.put(day, slots);
            }
        }
        return byDay;
    }

    /**
     * Turns a balance result (stopId to new day) into moves, renumbering each affected day's
     * positions 0..k-1 in the old (day, position) order. Untouched stops keep their relative order.
     */
    public static List<ItemMove> moves(Map<String, Integer> balance,
                                       List<ItineraryItemWithPlace> items) {
        List<ItineraryItemWithPlace> ordered = new ArrayList<>(items);
        Collections.sort(ordered, Comparator
                .comparingInt((ItineraryItemWithPlace e) -> e.item.getDay())
                .thenComparingInt(e -> e.item.getPosition()));
        Map<Integer, Integer> nextPosition = new LinkedHashMap<>();
        List<ItemMove> moves = new ArrayList<>();
        for (ItineraryItemWithPlace entry : ordered) {
            Integer target = balance.get(entry.item.getId());
            int day = target != null ? target : entry.item.getDay();
            int position = nextPosition.containsKey(day) ? nextPosition.get(day) : 0;
            nextPosition.put(day, position + 1);
            moves.add(new ItemMove(entry.item.getId(), day, position));
        }
        return Collections.unmodifiableList(moves);
    }
}
