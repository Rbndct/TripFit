package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BudgetMathTest {

    private static Expense expense(Category category, double home) {
        return new Expense("x", category, home, "PHP", home);
    }

    @Test
    public void totalSpent_sumsHomeAmounts() {
        List<Expense> list = Arrays.asList(expense(Category.FOOD, 100), expense(Category.OTHER, 50));
        assertEquals(150.0, BudgetMath.totalSpent(list), 0.0001);
    }

    @Test
    public void totalSpent_emptyList_isZero() {
        assertEquals(0.0, BudgetMath.totalSpent(Collections.emptyList()), 0.0001);
    }

    @Test
    public void spentByCategory_includesEveryCategory() {
        Map<Category, Double> totals = BudgetMath.spentByCategory(Arrays.asList(
                expense(Category.FOOD, 100), expense(Category.FOOD, 25)));
        assertEquals(125.0, totals.get(Category.FOOD), 0.0001);
        assertEquals(0.0, totals.get(Category.TRANSPORT), 0.0001);
        assertEquals(Category.values().length, totals.size());
    }

    @Test
    public void isOver_spendAboveLimit_isTrue() {
        assertTrue(BudgetMath.isOver(4293, 4000));
    }

    @Test
    public void isOver_spendAtLimit_isFalse() {
        assertFalse(BudgetMath.isOver(4000, 4000));
    }

    @Test
    public void isOver_noLimitSet_neverOver() {
        assertFalse(BudgetMath.isOver(999999, 0));
    }

    @Test
    public void percent_clampsToHundred() {
        assertEquals(100, BudgetMath.percent(5000, 4000));
        assertEquals(50, BudgetMath.percent(2000, 4000));
        assertEquals(0, BudgetMath.percent(100, 0));
    }

    @Test
    public void remaining_goesNegativeWhenOver() {
        assertEquals(-293.0, BudgetMath.remaining(4000, 4293), 0.0001);
    }
}
