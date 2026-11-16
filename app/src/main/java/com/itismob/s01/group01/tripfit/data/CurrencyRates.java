package com.itismob.s01.group01.tripfit.data;

import androidx.annotation.Nullable;

import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The app's current exchange-rate table. It starts as the built-in {@link CurrencyMock} fallback
 * and is swapped, atomically, for live Frankfurter rates once {@link CurrencyRepository} has
 * fetched (or restored from cache) a good snapshot. Every conversion and currency list in the UI
 * goes through here, so a refresh updates the whole app and an offline start still works.
 */
public final class CurrencyRates {

    /** Immutable rates (units per 1 USD), display names, and where they came from. */
    public static final class Snapshot {
        private final Map<String, Double> perUsd;
        private final Map<String, String> names;
        private final List<String> codes;
        private final String asOf;
        private final boolean live;

        private Snapshot(Map<String, Double> perUsd, Map<String, String> names, String asOf, boolean live) {
            this.perUsd = Collections.unmodifiableMap(new TreeMap<>(perUsd));
            this.names = Collections.unmodifiableMap(new HashMap<>(names));
            this.codes = Collections.unmodifiableList(new ArrayList<>(this.perUsd.keySet()));
            this.asOf = asOf;
            this.live = live;
        }

        /** Copies the inputs, so later changes to the caller's maps cannot alter the snapshot. */
        public static Snapshot of(Map<String, Double> perUsd, @Nullable Map<String, String> names,
                                  @Nullable String asOf, boolean live) {
            return new Snapshot(perUsd, names == null ? Collections.emptyMap() : names, asOf, live);
        }

        public Map<String, Double> perUsd() {
            return perUsd;
        }

        public Map<String, String> names() {
            return names;
        }

        @Nullable
        public String asOf() {
            return asOf;
        }

        public boolean isLive() {
            return live;
        }
    }

    private static volatile Snapshot current = fallback();

    private CurrencyRates() { }

    private static Snapshot fallback() {
        return Snapshot.of(CurrencyMock.perUsd(), null, null, false);
    }

    public static Snapshot current() {
        return current;
    }

    public static void update(Snapshot snapshot) {
        current = snapshot;
    }

    /** Back to the built-in demo table (used when nothing live is available, and by tests). */
    public static void useFallback() {
        current = fallback();
    }

    /** Selectable currency codes, sorted alphabetically. */
    public static List<String> codes() {
        return current.codes;
    }

    /** The currency's English name, or the code itself when no name is known. */
    public static String nameOf(String code) {
        String name = current.names.get(code);
        return name == null ? code : name;
    }

    public static boolean isLive() {
        return current.live;
    }

    /** ISO date of the rates when live, otherwise null. */
    @Nullable
    public static String asOfDate() {
        return current.asOf;
    }

    /** Converts between any two known currencies via USD; unknown codes pass through unchanged. */
    public static double convert(double amount, String from, String to) {
        if (from.equals(to)) {
            return amount;
        }
        Snapshot snapshot = current;
        Double fromRate = snapshot.perUsd.get(from);
        Double toRate = snapshot.perUsd.get(to);
        if (fromRate == null || toRate == null) {
            return amount;
        }
        return amount / fromRate * toRate;
    }

    /** Formats like "₱1,234", or "CHF 1,234" when the currency has no known symbol. */
    public static String format(double amount, String code) {
        return CurrencyMock.format(amount, code);
    }
}
