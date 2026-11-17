package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public class BudgetSettingsTest {

    private static BudgetSettings budget(boolean perDay) {
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        limits.put(Category.FOOD, 100.0);
        return new BudgetSettings("PHP", "JPY", 1000, perDay, limits);
    }

    @Test
    public void limitFor_overallModeIgnoresDays() {
        assertEquals(100.0, budget(false).limitFor(Category.FOOD, 5), 0.0);
    }

    @Test
    public void limitFor_dailyModeMultipliesByDays() {
        assertEquals(500.0, budget(true).limitFor(Category.FOOD, 5), 0.0);
    }

    @Test
    public void limitFor_dailyModeTreatsZeroDaysAsOne() {
        assertEquals(100.0, budget(true).limitFor(Category.FOOD, 0), 0.0);
    }

    @Test
    public void limitFor_unsetCategoryStaysNoLimit() {
        assertEquals(0.0, budget(true).limitFor(Category.OTHER, 5), 0.0);
    }

    @Test
    public void limitFor_withoutDaysStaysTheStoredValue() {
        assertEquals(100.0, budget(true).limitFor(Category.FOOD), 0.0);
    }
}
