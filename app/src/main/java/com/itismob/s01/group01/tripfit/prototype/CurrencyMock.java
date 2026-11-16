package com.itismob.s01.group01.tripfit.prototype;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fixed demo exchange rates (units per 1 USD). {@link com.itismob.s01.group01.tripfit.data.CurrencyRates}
 * uses these only as the offline fallback until live Frankfurter rates are fetched or restored from cache,
 * and still delegates symbol formatting here.
 */
public final class CurrencyMock {

    private static final Map<String, Double> PER_USD = new HashMap<>();
    private static final Map<String, String> SYMBOLS = new HashMap<>();
    private static final List<String> CODES = Collections.unmodifiableList(
            Arrays.asList("PHP", "USD", "JPY", "EUR", "KRW", "SGD"));

    static {
        PER_USD.put("USD", 1.0);
        PER_USD.put("PHP", 56.0);
        PER_USD.put("JPY", 150.0);
        PER_USD.put("EUR", 0.92);
        PER_USD.put("KRW", 1350.0);
        PER_USD.put("SGD", 1.34);
        SYMBOLS.put("USD", "$");
        SYMBOLS.put("PHP", "₱");
        SYMBOLS.put("JPY", "¥");
        SYMBOLS.put("EUR", "€");
        SYMBOLS.put("KRW", "₩");
        SYMBOLS.put("SGD", "S$");
    }

    private CurrencyMock() { }

    public static List<String> codes() {
        return CODES;
    }

    /** Read-only copy of the built-in rates (units per 1 USD): the offline fallback for CurrencyRates. */
    public static Map<String, Double> perUsd() {
        return Collections.unmodifiableMap(new HashMap<>(PER_USD));
    }

    /** Converts {@code amount} between two supported currencies; unknown codes pass through unchanged. */
    public static double convert(double amount, String from, String to) {
        Double fromRate = PER_USD.get(from);
        Double toRate = PER_USD.get(to);
        if (fromRate == null || toRate == null) {
            return amount;
        }
        return amount / fromRate * toRate;
    }

    /** Formats like "₱1,234.50": a standard two decimals for every currency. */
    public static String format(double amount, String code) {
        String symbol = SYMBOLS.containsKey(code) ? SYMBOLS.get(code) : code + " ";
        return symbol + String.format(Locale.US, "%,.2f", amount);
    }
}
