package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/** Planned (stop estimate) totals and conversion; never mixed with logged expenses. */
public class PlannedCostsTest {

    @Before
    public void useSampleRates() {
        CurrencyRates.useFallback();
    }

    private static TripSnapshot trip(TripSnapshot.Stop... stops) {
        TripSnapshot trip = new TripSnapshot();
        trip.city = "Tokyo";
        trip.homeCurrency = "PHP";
        trip.stops.addAll(Arrays.asList(stops));
        return trip;
    }

    private static TripSnapshot.Stop stop(double cost, String currency, String category) {
        TripSnapshot.Stop stop = new TripSnapshot.Stop();
        stop.cost = cost;
        stop.currency = currency;
        stop.category = category;
        return stop;
    }

    @Test
    public void convert_usesTheSingleRatesTable() {
        assertEquals(56.0, BudgetMath.convert(150, "JPY", "PHP"), 0.0001);
        assertEquals(42.0, BudgetMath.convert(42, "XXX", "PHP"), 0.0001);
    }

    @Test
    public void routePreviewConvert_delegatesToTheSameTableAndPassesUnknownCodesThrough() {
        assertEquals(CurrencyRates.convert(1234, "USD", "PHP"),
                RoutePreview.convert(1234, "USD", "PHP"), 0.0);
        assertEquals(1234 * 56.0, RoutePreview.convert(1234, "USD", "PHP"), 0.0001);
        assertEquals(42.0, RoutePreview.convert(42, "XXX", "PHP"), 0.0);
        assertEquals(42.0, RoutePreview.convert(42, "PHP", "XXX"), 0.0);
    }

    @Test
    public void plannedTotal_convertsEachStopFromItsOwnCurrency() {
        TripSnapshot trip = trip(stop(150, "JPY", "Food"), stop(100, "PHP", "Activities"));
        assertEquals(156.0, BudgetMath.plannedTotal(trip), 0.0001);
    }

    @Test
    public void plannedTotal_stopWithoutCurrencyUsesTheTripsLocalCurrency() {
        TripSnapshot trip = trip(stop(150, "", "Food"));
        assertEquals(56.0, BudgetMath.plannedTotal(trip), 0.0001);
    }

    @Test
    public void plannedTotal_noStopsIsZero() {
        assertEquals(0.0, BudgetMath.plannedTotal(trip()), 0.0);
    }

    @Test
    public void plannedByCategory_groupsAndIncludesEveryCategory() {
        TripSnapshot trip = trip(stop(150, "JPY", "Food"), stop(300, "JPY", "Food"),
                stop(100, "PHP", "Transport"), stop(5, "PHP", "mystery"));
        Map<Category, Double> byCategory = BudgetMath.plannedByCategory(trip);
        assertEquals(Category.values().length, byCategory.size());
        assertEquals(168.0, byCategory.get(Category.FOOD), 0.0001);
        assertEquals(100.0, byCategory.get(Category.TRANSPORT), 0.0001);
        assertEquals(0.0, byCategory.get(Category.ACTIVITIES), 0.0001);
        assertEquals(5.0, byCategory.get(Category.OTHER), 0.0001);
    }

    @Test
    public void plannedAndLoggedAreSeparateNumbers() {
        TripSnapshot trip = trip(stop(150, "JPY", "Food"));
        Expense logged = new Expense("Ramen", Category.FOOD, 300, "JPY", 112);
        double planned = BudgetMath.plannedTotal(trip);
        double spent = BudgetMath.totalSpent(Arrays.asList(logged));
        assertEquals(56.0, planned, 0.0001);
        assertEquals(112.0, spent, 0.0001);
        assertEquals(56.0, BudgetMath.plannedByCategory(trip).get(Category.FOOD), 0.0001);
        assertEquals(112.0, BudgetMath.spentByCategory(Arrays.asList(logged)).get(Category.FOOD),
                0.0001);
    }

    @Test
    public void plannedOverBudget_onlyWhenAboveATrueLimit() {
        assertTrue(BudgetMath.isOver(1000.01, 1000));
        assertFalse(BudgetMath.isOver(1000, 1000));
        assertFalse(BudgetMath.isOver(5000, 0));
    }

    @Test
    public void categoryNames_roundTripAndFallBackToOther() {
        for (Category category : Category.values()) {
            assertEquals(category, BudgetMath.parseCategory(BudgetMath.categoryName(category)));
        }
        assertEquals("Activities", BudgetMath.categoryName(Category.ACTIVITIES));
        assertEquals(Category.FOOD, BudgetMath.parseCategory("food"));
        assertEquals(Category.OTHER, BudgetMath.parseCategory(null));
        assertEquals(Category.OTHER, BudgetMath.parseCategory("zzz"));
    }

    @Test
    public void explicitStopCurrencyStaysAfterTheCityChanges() {
        TripSnapshot trip = trip(stop(150, "JPY", "Food"));
        TripSnapshot.Stop stop = trip.stops.get(0);
        assertEquals("JPY", trip.costCurrency(stop));
        trip.city = "Manila";
        assertEquals("JPY", trip.costCurrency(stop));
        assertEquals(56.0, trip.convertedCost(stop), 0.0001);
    }

    @Test
    public void localCurrencyFollowsTheSameRuleAsBudgetSettings() {
        for (String city : new String[]{"Tokyo", "Manila", "Seoul"}) {
            TripSnapshot trip = new TripSnapshot();
            trip.city = city;
            assertEquals(MockData.currencyFor(city), trip.localCurrency());
        }
    }

    @Test
    public void unknownCurrencies_listsStopsAndExpensesOutsideTheTable() {
        TripSnapshot trip = trip(stop(10, "XXX", "Food"), stop(10, "JPY", "Food"),
                stop(0, "ZZZ", "Food"));
        Expense odd = new Expense("Odd", Category.FOOD, 5, "QQQ", 5);
        Expense fine = new Expense("Fine", Category.FOOD, 5, "USD", 280);
        assertEquals(new java.util.TreeSet<>(Arrays.asList("QQQ", "XXX")),
                BudgetMath.unknownCurrencies(trip, Arrays.asList(odd, fine)));
        assertTrue(BudgetMath.unknownCurrencies(trip(stop(1, "JPY", "Food")),
                java.util.Collections.<Expense>emptyList()).isEmpty());
    }

    @Test
    public void currencyRatesHas_knowsOnlyTableCodes() {
        assertTrue(CurrencyRates.has("JPY"));
        assertFalse(CurrencyRates.has("XXX"));
        assertFalse(CurrencyRates.has(null));
    }

    @Test
    public void plannedVersusTripTotal_dailyBudgetUsesDays() {
        BudgetSettings daily = new BudgetSettings("PHP", "JPY", 100, true,
                java.util.Collections.<Category, Double>emptyMap());
        TripSnapshot trip = trip(stop(250, "PHP", "Food"));
        assertTrue(BudgetMath.isOver(BudgetMath.plannedTotal(trip), daily.tripTotal(2)));
        assertFalse(BudgetMath.isOver(BudgetMath.plannedTotal(trip), daily.tripTotal(3)));
    }
}
