package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;

public class DefaultLimitsTest {

    private static double sum(Map<Category, Double> limits) {
        double total = 0;
        for (double v : limits.values()) {
            total += v;
        }
        return total;
    }

    @Test
    public void defaultLimits_splitsThirtyFortyTwentyTen() {
        Map<Category, Double> limits = BudgetMath.defaultLimits(15000);

        assertEquals(4500.0, limits.get(Category.FOOD), 0.0);
        assertEquals(6000.0, limits.get(Category.ACTIVITIES), 0.0);
        assertEquals(3000.0, limits.get(Category.TRANSPORT), 0.0);
        assertEquals(1500.0, limits.get(Category.OTHER), 0.0);
    }

    @Test
    public void defaultLimits_partsAlwaysSumToTotalAfterRounding() {
        double[] totals = {1, 0.01, 99.99, 100.01, 333.33, 1234.57, 100000000};
        for (double total : totals) {
            assertEquals("total " + total, total, sum(BudgetMath.defaultLimits(total)), 0.0001);
        }
    }

    @Test
    public void defaultLimits_valuesAreTwoDecimals() {
        for (double v : BudgetMath.defaultLimits(100.01).values()) {
            assertEquals(v, Math.round(v * 100) / 100.0, 1e-9);
        }
    }
}
