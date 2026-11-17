package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Running-budget calculations for the tracker screen. */
public final class BudgetMath {

    private BudgetMath() { }

    public static double totalSpent(List<Expense> expenses) {
        double total = 0;
        for (Expense expense : expenses) {
            total += expense.getAmountHome();
        }
        return total;
    }

    public static Map<Category, Double> spentByCategory(List<Expense> expenses) {
        Map<Category, Double> totals = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            totals.put(category, 0.0);
        }
        for (Expense expense : expenses) {
            totals.put(expense.getCategory(),
                    totals.get(expense.getCategory()) + expense.getAmountHome());
        }
        return totals;
    }

    // ---- planned costs (stop estimates), always kept apart from logged expenses ----

    /** Converts between currencies with the app's single rates table; unknown codes pass through. */
    public static double convert(double amount, String from, String to) {
        return CurrencyRates.convert(amount, from, to);
    }

    /** Sum of every stop's estimate converted to the trip's home currency. */
    public static double plannedTotal(DraftTrip trip) {
        double total = 0;
        for (DraftTrip.Stop stop : trip.stops) {
            total += trip.convertedCost(stop);
        }
        return total;
    }

    /** Planned estimates per category in home currency; every category is present. */
    public static Map<Category, Double> plannedByCategory(DraftTrip trip) {
        Map<Category, Double> totals = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            totals.put(category, 0.0);
        }
        for (DraftTrip.Stop stop : trip.stops) {
            Category category = parseCategory(stop.category);
            totals.put(category, totals.get(category) + trip.convertedCost(stop));
        }
        return totals;
    }

    /** The persisted/exported name of a category ("Food", "Activities", "Transport", "Other"). */
    public static String categoryName(Category category) {
        String name = category.name();
        return name.charAt(0) + name.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    /** Case-insensitive inverse of {@link #categoryName}; unknown or missing names are Other. */
    public static Category parseCategory(String name) {
        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return Category.OTHER;
    }

    /** Currency codes (sorted) used by a costed stop or an expense but missing from the rates table. */
    public static java.util.Set<String> unknownCurrencies(DraftTrip trip, List<Expense> expenses) {
        java.util.Set<String> unknown = new java.util.TreeSet<>();
        for (DraftTrip.Stop stop : trip.stops) {
            String code = trip.costCurrency(stop);
            if (stop.cost > 0 && !CurrencyRates.has(code)) {
                unknown.add(code);
            }
        }
        for (Expense expense : expenses) {
            if (!CurrencyRates.has(expense.getLocalCurrency())) {
                unknown.add(expense.getLocalCurrency());
            }
        }
        return unknown;
    }

    /** True when every limit equals the 30/40/20/10 default split of {@code amount} (to the cent). */
    public static boolean limitsMatchDefaults(Map<Category, Double> limits, double amount) {
        Map<Category, Double> defaults = defaultLimits(amount);
        for (Category category : Category.values()) {
            Double limit = limits.get(category);
            double value = limit == null ? 0 : limit;
            if (Math.round(value * 100) != Math.round(defaults.get(category) * 100)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Limits after the budget changed from {@code oldAmount} to {@code newAmount}: the default
     * split of the new amount when the limits were still the old default, else unchanged.
     */
    public static Map<Category, Double> followDefaultSplit(Map<Category, Double> limits,
                                                           double oldAmount, double newAmount) {
        return limitsMatchDefaults(limits, oldAmount) ? defaultLimits(newAmount) : limits;
    }

    /** A limit of 0 means "no limit set", which can never be exceeded. */
    public static boolean isOver(double spent, double limit) {
        return limit > 0 && spent > limit;
    }

    public static double remaining(double limit, double spent) {
        return limit - spent;
    }

    /** Progress-bar value clamped to 0..100; an unset limit shows an empty bar. */
    public static int percent(double spent, double limit) {
        if (limit <= 0) {
            return 0;
        }
        return (int) Math.max(0, Math.min(100, Math.round(spent / limit * 100)));
    }

    /**
     * Default category limits for a new budget: Food 30%, Activities 40%, Transport 20%, Other
     * 10%, each rounded to two decimals. Other takes the remainder so the parts sum to the total.
     */
    public static Map<Category, Double> defaultLimits(double total) {
        double food = round2(total * 0.30);
        double activities = round2(total * 0.40);
        double transport = round2(total * 0.20);
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        limits.put(Category.FOOD, food);
        limits.put(Category.ACTIVITIES, activities);
        limits.put(Category.TRANSPORT, transport);
        limits.put(Category.OTHER, round2(total - food - activities - transport));
        return limits;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
