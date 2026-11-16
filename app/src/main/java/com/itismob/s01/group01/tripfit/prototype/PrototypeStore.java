package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Session-only, in-memory state for the UI prototype's not-yet-backed features (expenses, budget
 * setup, per-leg modes, journal, votes, calendar sync). Everything resets when the process dies;
 * real persistence replaces this once the backend lands. Main-thread access only.
 */
public final class PrototypeStore {

    private static final PrototypeStore INSTANCE = new PrototypeStore();
    private static final String DEFAULT_CITY = "Tokyo";
    private static final int DEFAULT_BUFFER_MIN = 15;
    private static final int DEFAULT_TRIP_DAYS = 2;
    private static final double BASE_BUDGET_PHP = 15000;

    private final Map<String, List<Expense>> expenses = new HashMap<>();
    private final Map<String, BudgetSettings> budgets = new HashMap<>();
    private final Map<String, TripMeta> metas = new HashMap<>();
    private final Map<String, TravelMode> legModes = new HashMap<>();
    private final Map<String, List<JournalEntry>> journals = new HashMap<>();
    private final Set<String> myVotes = new HashSet<>();
    private final Set<String> syncedTrips = new HashSet<>();
    private String defaultHomeCurrency = "PHP";
    private int bufferMin = DEFAULT_BUFFER_MIN;

    private PrototypeStore() { }

    public static PrototypeStore get() {
        return INSTANCE;
    }

    // ---- settings ----

    public String getDefaultHomeCurrency() {
        return defaultHomeCurrency;
    }

    public void setDefaultHomeCurrency(String code) {
        defaultHomeCurrency = code;
    }

    public int getBufferMin() {
        return bufferMin;
    }

    public void setBufferMin(int minutes) {
        bufferMin = minutes;
    }

    // ---- trip meta ----

    public TripMeta meta(String tripId) {
        TripMeta meta = metas.get(tripId);
        return meta != null ? meta : new TripMeta(DEFAULT_CITY, "", DEFAULT_TRIP_DAYS, defaultHomeCurrency);
    }

    public void setMeta(String tripId, TripMeta meta) {
        metas.put(tripId, meta);
    }

    // ---- budget & expenses ----

    public BudgetSettings budget(String tripId) {
        BudgetSettings existing = budgets.get(tripId);
        if (existing != null) {
            return existing;
        }
        TripMeta meta = meta(tripId);
        return defaultBudget(meta.getHomeCurrency(), MockData.currencyFor(meta.getCity()));
    }

    public void setBudget(String tripId, BudgetSettings settings) {
        budgets.put(tripId, settings);
    }

    /** Expenses newest-first, re-converted to the trip's current home currency. */
    public List<Expense> expenses(String tripId) {
        seedExpensesIfNeeded(tripId);
        String home = budget(tripId).getHomeCurrency();
        List<Expense> converted = new ArrayList<>();
        for (Expense expense : expenses.get(tripId)) {
            converted.add(new Expense(expense.getTitle(), expense.getCategory(),
                    expense.getAmountLocal(), expense.getLocalCurrency(),
                    CurrencyRates.convert(expense.getAmountLocal(), expense.getLocalCurrency(), home)));
        }
        return converted;
    }

    /** Logs an expense entered in the trip's local currency, converting it to home currency. */
    public Expense addExpense(String tripId, String title, Category category, double amountLocal,
                              String localCurrency) {
        BudgetSettings budget = budget(tripId);
        Expense expense = new Expense(title, category, amountLocal, localCurrency,
                CurrencyRates.convert(amountLocal, localCurrency, budget.getHomeCurrency()));
        seedExpensesIfNeeded(tripId);
        expenses.get(tripId).add(0, expense);
        return expense;
    }

    private void seedExpensesIfNeeded(String tripId) {
        if (expenses.containsKey(tripId)) {
            return;
        }
        List<Expense> list = new ArrayList<>();
        if (TripRepositoryImpl.SEED_TRIP_ID.equals(tripId)) {
            String home = budget(tripId).getHomeCurrency();
            list.add(seedExpense("Suica top-up", Category.TRANSPORT, 2000, home));
            list.add(seedExpense("teamLab Planets tickets", Category.ACTIVITIES, 3800, home));
            list.add(seedExpense("Sushi lunch", Category.FOOD, 8500, home));
            list.add(seedExpense("Ramen dinner", Category.FOOD, 3000, home));
        }
        expenses.put(tripId, list);
    }

    private Expense seedExpense(String title, Category category, double jpy, String home) {
        return new Expense(title, category, jpy, "JPY", CurrencyRates.convert(jpy, "JPY", home));
    }

    private BudgetSettings defaultBudget(String home, String local) {
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        limits.put(Category.FOOD, scale(4000, home));
        limits.put(Category.ACTIVITIES, scale(5000, home));
        limits.put(Category.TRANSPORT, scale(3000, home));
        limits.put(Category.OTHER, scale(3000, home));
        return new BudgetSettings(home, local, scale(BASE_BUDGET_PHP, home), false, limits);
    }

    /** Converts a PHP-denominated demo figure into {@code home}, rounded to the nearest 100. */
    private static double scale(double php, String home) {
        double converted = CurrencyRates.convert(php, "PHP", home);
        return Math.max(100, Math.round(converted / 100.0) * 100.0);
    }

    // ---- per-leg transport ----

    public TravelMode legMode(String itemId, double km) {
        TravelMode mode = legModes.get(itemId);
        return mode != null ? mode : TravelMode.suggestFor(km);
    }

    public void setLegMode(String itemId, TravelMode mode) {
        legModes.put(itemId, mode);
    }

    /** Deterministic fake leg distance (km) from an item id, standing in for the Matrix API. */
    public static double legKm(String itemId) {
        return 0.4 + (Math.abs(itemId.hashCode()) % 90) / 10.0;
    }

    // ---- journal ----

    public List<JournalEntry> journal(String tripId) {
        if (!journals.containsKey(tripId)) {
            List<JournalEntry> seed = new ArrayList<>();
            if (TripRepositoryImpl.SEED_TRIP_ID.equals(tripId)) {
                seed.add(new JournalEntry(1, "Senso-ji Temple",
                        "Got there early — no crowds. Drew an omikuji fortune: small blessing.",
                        true));
            }
            journals.put(tripId, seed);
        }
        return new ArrayList<>(journals.get(tripId));
    }

    public void addJournalEntry(String tripId, JournalEntry entry) {
        List<JournalEntry> current = journal(tripId);
        current.add(0, entry);
        journals.put(tripId, current);
    }

    // ---- group votes ----

    /** Demo vote tally: a stable 2-4 base per destination plus the user's own vote. */
    public int voteCount(String destination) {
        int base = 2 + Math.abs(destination.hashCode()) % 3;
        return base + (myVotes.contains(destination) ? 1 : 0);
    }

    public boolean hasVoted(String destination) {
        return myVotes.contains(destination);
    }

    public void toggleVote(String destination) {
        if (!myVotes.remove(destination)) {
            myVotes.add(destination);
        }
    }

    // ---- calendar sync ----

    public boolean isSynced(String tripId) {
        return syncedTrips.contains(tripId);
    }

    public void markSynced(String tripId) {
        syncedTrips.add(tripId);
    }
}
