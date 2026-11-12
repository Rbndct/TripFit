package com.itismob.s01.group01.tripfit.prototype;

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
}
