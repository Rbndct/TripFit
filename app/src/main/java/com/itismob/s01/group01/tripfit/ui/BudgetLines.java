package com.itismob.s01.group01.tripfit.ui;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;

/** Pure text composition for the Budget screen; templates come from string resources. */
public final class BudgetLines {

    private BudgetLines() { }

    /**
     * The category card's amount line: "X remaining of Y", "Over budget by X of Y" once the limit
     * is exceeded, or the no-limit line when no limit is set.
     *
     * @param remainingTemplate {@code %1$s} remaining, {@code %2$s} limit
     * @param overTemplate      {@code %1$s} overspend, {@code %2$s} limit
     * @param noLimitTemplate   {@code %1$s} spent
     */
    public static String categoryLine(double spent, double limit, String home,
                                      String remainingTemplate, String overTemplate,
                                      String noLimitTemplate) {
        if (limit <= 0) {
            return String.format(noLimitTemplate, CurrencyRates.format(spent, home));
        }
        double left = BudgetMath.remaining(limit, spent);
        if (BudgetMath.isOver(spent, limit)) {
            return String.format(overTemplate, CurrencyRates.format(-left, home),
                    CurrencyRates.format(limit, home));
        }
        return String.format(remainingTemplate, CurrencyRates.format(left, home),
                CurrencyRates.format(limit, home));
    }
}
