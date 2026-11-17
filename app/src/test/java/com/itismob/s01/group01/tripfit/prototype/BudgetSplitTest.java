package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public class BudgetSplitTest {

    @Test
    public void limitsMatchDefaults_trueForTheDefaultSplitOnly() {
        Map<Category, Double> limits = BudgetMath.defaultLimits(15000);
        assertTrue(BudgetMath.limitsMatchDefaults(limits, 15000));
        assertFalse(BudgetMath.limitsMatchDefaults(limits, 20000));
        limits.put(Category.FOOD, 4500.01);
        assertFalse(BudgetMath.limitsMatchDefaults(limits, 15000));
    }

    @Test
    public void limitsMatchDefaults_emptyLimitsMatchAZeroBudget() {
        assertTrue(BudgetMath.limitsMatchDefaults(new EnumMap<>(Category.class), 0));
    }

    @Test
    public void followDefaultSplit_recomputesWhenLimitsAreStillTheDefault() {
        Map<Category, Double> next = BudgetMath.followDefaultSplit(
                BudgetMath.defaultLimits(1000), 1000, 2000);
        assertEquals(BudgetMath.defaultLimits(2000), next);
    }

    @Test
    public void followDefaultSplit_leavesUserNumbersAlone() {
        Map<Category, Double> custom = BudgetMath.defaultLimits(1000);
        custom.put(Category.FOOD, 1.0);
        assertEquals(custom, BudgetMath.followDefaultSplit(custom, 1000, 2000));
    }

    @Test
    public void followDefaultSplit_dailyAmountsFollowThePerDayAmount() {
        Map<Category, Double> next = BudgetMath.followDefaultSplit(
                BudgetMath.defaultLimits(100), 100, 150);
        assertEquals(45.0, next.get(Category.FOOD), 0.0);
        assertEquals(15.0, next.get(Category.OTHER), 0.0);
    }
}
