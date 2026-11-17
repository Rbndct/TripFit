package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class PrototypeStoreBudgetTest {

    private InMemoryKeyValueStorage storage;
    private PrototypeStore store;

    @Before
    public void setUp() {
        CurrencyRates.useFallback();
        storage = new InMemoryKeyValueStorage();
        store = PrototypeStore.forStorage(storage);
    }

    private PrototypeStore reload() {
        return PrototypeStore.forStorage(storage);
    }

    @Test
    public void removeExpense_removesOnlyThatExpenseAndPersists() {
        Expense keep = store.addExpense("t1", "Ramen", Category.FOOD, 900, "JPY");
        Expense drop = store.addExpense("t1", "Taxi", Category.TRANSPORT, 1500, "JPY");

        assertTrue(store.removeExpense("t1", drop.getId()));

        List<Expense> remaining = reload().expenses("t1");
        assertEquals(1, remaining.size());
        assertEquals(keep.getId(), remaining.get(0).getId());
    }

    @Test
    public void removeExpense_unknownIdChangesNothing() {
        store.addExpense("t1", "Ramen", Category.FOOD, 900, "JPY");
        assertFalse(store.removeExpense("t1", "nope"));
        assertFalse(store.removeExpense("other-trip", "nope"));
        assertEquals(1, store.expenses("t1").size());
    }

    @Test
    public void removeExpense_worksOnTheSeededSampleTrip() {
        String tripId = com.itismob.s01.group01.tripfit.data.TripRepositoryImpl.SEED_TRIP_ID;
        List<Expense> before = store.expenses(tripId);
        assertTrue(store.removeExpense(tripId, before.get(0).getId()));
        assertEquals(before.size() - 1, reload().expenses(tripId).size());
    }

    @Test
    public void expenses_keepTheirOriginalCurrencyAndConvertToHome() {
        store.setMeta("t1", new TripMeta("Tokyo", "", 3, "PHP"));
        store.addExpense("t1", "Coffee", Category.FOOD, 150, "JPY");
        store.addExpense("t1", "Souvenir", Category.OTHER, 10, "USD");

        List<Expense> loaded = reload().expenses("t1");
        Expense usd = loaded.get(0);
        Expense jpy = loaded.get(1);
        assertEquals("USD", usd.getLocalCurrency());
        assertEquals(10.0, usd.getAmountLocal(), 0.0);
        assertEquals(560.0, usd.getAmountHome(), 0.0001);
        assertEquals("JPY", jpy.getLocalCurrency());
        assertEquals(56.0, jpy.getAmountHome(), 0.0001);
    }

    @Test
    public void itemDetails_costCurrencyAndCategoryRoundTripByItemId() {
        store.setItemDetails("stop-1", ItemDetails.withCost(null, 1500.25, "JPY", "Food"));

        ItemDetails loaded = reload().itemDetails("stop-1");
        assertNotNull(loaded);
        assertEquals(1500.25, loaded.getCost(), 0.0);
        assertEquals("JPY", loaded.getCostCurrency());
        assertEquals("Food", loaded.getCategory());
        assertNull(reload().itemDetails("stop-2"));
    }

    @Test
    public void defaultBudget_usesTheThirtyFortyTwentyTenSplit() {
        store.setMeta("t1", new TripMeta("Tokyo", "", 3, "PHP"));

        BudgetSettings budget = store.budget("t1");

        Map<Category, Double> expected = BudgetMath.defaultLimits(budget.getAmount());
        for (Category category : Category.values()) {
            assertEquals(expected.get(category), budget.limitFor(category), 0.0);
        }
        assertEquals(budget.getAmount() * 0.30, budget.limitFor(Category.FOOD), 0.01);
    }

    @Test
    public void defaultBudget_splitFollowsTheHomeCurrencyAmount() {
        store.setMeta("t1", new TripMeta("Tokyo", "", 3, "USD"));

        BudgetSettings budget = store.budget("t1");

        double sum = 0;
        for (Category category : Category.values()) {
            sum += budget.limitFor(category);
        }
        assertEquals(budget.getAmount(), sum, 0.0001);
    }

    @Test
    public void removeExpense_worksForARecordSavedWithoutAnId() {
        storage.putString("trip.n", "{\"expenses\":[{\"title\":\"x\",\"category\":\"FOOD\","
                + "\"localCurrency\":\"PHP\",\"amountLocal\":1.0}]}");

        PrototypeStore fresh = reload();
        String id = fresh.expenses("n").get(0).getId();

        assertTrue(fresh.removeExpense("n", id));
        assertTrue(reload().expenses("n").isEmpty());
    }
}
