package com.itismob.s01.group01.tripfit.prototype;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable per-trip budget: overall or per-day amount, optional category limits, currencies. */
public final class BudgetSettings {

    private final String homeCurrency;
    private final String localCurrency;
    private final double amount;
    private final boolean perDay;
    private final Map<Category, Double> categoryLimits;

    public BudgetSettings(String homeCurrency, String localCurrency, double amount,
                          boolean perDay, Map<Category, Double> categoryLimits) {
        this.homeCurrency = homeCurrency;
        this.localCurrency = localCurrency;
        this.amount = amount;
        this.perDay = perDay;
        Map<Category, Double> copy = new EnumMap<>(Category.class);
        copy.putAll(categoryLimits);
        this.categoryLimits = Collections.unmodifiableMap(copy);
    }

    public String getHomeCurrency() {
        return homeCurrency;
    }

    public String getLocalCurrency() {
        return localCurrency;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isPerDay() {
        return perDay;
    }

    public Map<Category, Double> getCategoryLimits() {
        return categoryLimits;
    }

    /** Total trip budget: the per-day amount times the trip length, or the overall amount. */
    public double tripTotal(int days) {
        return perDay ? amount * Math.max(1, days) : amount;
    }

    /** Limit for one category, or 0 when no split was set (0 means "no limit"). */
    public double limitFor(Category category) {
        Double limit = categoryLimits.get(category);
        return limit == null ? 0 : limit;
    }

    /**
     * Limit for one category over the whole trip: in daily mode the stored limit is per day, so it
     * is multiplied by the trip length (at least 1). 0 still means "no limit".
     */
    public double limitFor(Category category, int days) {
        double limit = limitFor(category);
        return perDay ? limit * Math.max(1, days) : limit;
    }
}
