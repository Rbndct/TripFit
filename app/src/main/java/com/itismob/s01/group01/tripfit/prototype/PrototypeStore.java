package com.itismob.s01.group01.tripfit.prototype;

import android.content.Context;

import androidx.annotation.Nullable;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent state for the prototype's not-yet-Room-backed features (expenses, budget setup, trip
 * meta, per-leg modes and buffers, journal, per-trip votes and members, weather, calendar sync,
 * per-item details). One JSON document per trip id plus one global document, kept in a
 * {@link KeyValueStorage} (SharedPreferences file {@code trippings_store_v2} in the app).
 *
 * <p>Call {@link #init(Context)} once from {@code Application.onCreate}. Without it {@link #get()}
 * falls back to non-persistent in-memory storage. Every public method is synchronized, so it is
 * safe from the main thread and from callbacks; writes use SharedPreferences {@code apply()}.
 * Collections returned are unmodifiable copies.
 */
public final class PrototypeStore {

    /** Vote scope used by the legacy trip-less vote methods. */
    public static final String DEFAULT_VOTE_SCOPE = "default";

    private static final String DEFAULT_CITY = "Tokyo";
    private static final String DEFAULT_HOME_CURRENCY = "PHP";
    private static final String SELF_MEMBER = "You";
    private static final String SAMPLE_MEMBER = "Alex (sample)";
    private static final int DEFAULT_BUFFER_MIN = 15;
    private static final int DEFAULT_TRIP_DAYS = 2;
    private static final double BASE_BUDGET_PHP = 15000;
    private static final String SEED_JOURNAL_ID = "seed-journal-1";

    private static volatile PrototypeStore instance;

    private final DocumentStore docs;
    private final boolean persistent;

    private PrototypeStore(KeyValueStorage storage, boolean persistent) {
        this.docs = new DocumentStore(storage);
        this.persistent = persistent;
    }

    /** Test seam: a store over the given storage; a second one over the same storage reloads. */
    static PrototypeStore forStorage(KeyValueStorage storage) {
        return new PrototypeStore(storage, false);
    }

    /**
     * Switches the app to persistent storage. Call before anything else uses the store. A no-op
     * when a persistent store already exists, so cached data is never discarded.
     */
    public static synchronized void init(Context context) {
        if (instance != null && instance.persistent) {
            return;
        }
        instance = new PrototypeStore(new PrefsKeyValueStorage(context), true);
    }

    /** Test seam: drops the shared instance so the next {@link #get()} starts empty. */
    @androidx.annotation.VisibleForTesting
    public static synchronized void resetForTesting() {
        instance = null;
    }

    public static PrototypeStore get() {
        PrototypeStore current = instance;
        if (current == null) {
            synchronized (PrototypeStore.class) {
                if (instance == null) {
                    instance = new PrototypeStore(new InMemoryKeyValueStorage(), false);
                }
                current = instance;
            }
        }
        return current;
    }

    // ---- settings ----

    public synchronized String getDefaultHomeCurrency() {
        String code = docs.global().homeCurrency;
        return code != null ? code : DEFAULT_HOME_CURRENCY;
    }

    public synchronized void setDefaultHomeCurrency(String code) {
        docs.global().homeCurrency = code;
        docs.saveGlobal();
    }

    public synchronized int getBufferMin() {
        Integer minutes = docs.global().bufferMin;
        return minutes != null ? minutes : DEFAULT_BUFFER_MIN;
    }

    public synchronized void setBufferMin(int minutes) {
        docs.global().bufferMin = minutes;
        docs.saveGlobal();
    }

    // ---- trip meta, sample flag, members, weather ----

    public synchronized TripMeta meta(String tripId) {
        TripDoc.MetaRec rec = docs.trip(tripId).meta;
        if (rec == null) {
            return new TripMeta(DEFAULT_CITY, "", DEFAULT_TRIP_DAYS, getDefaultHomeCurrency());
        }
        return TripDocMapper.fromRec(rec, DEFAULT_CITY, DEFAULT_TRIP_DAYS, getDefaultHomeCurrency());
    }

    /** True once trip details were saved for this trip (as opposed to the defaults of {@link #meta}). */
    public synchronized boolean hasMeta(String tripId) {
        return docs.trip(tripId).meta != null;
    }

    public synchronized void setMeta(String tripId, TripMeta meta) {
        docs.trip(tripId).meta = TripDocMapper.toRec(meta);
        docs.saveTrip(tripId);
    }

    /** True for the seeded demo trip or any trip flagged with {@link #setSample}. */
    public synchronized boolean isSample(String tripId) {
        return TripRepositoryImpl.SEED_TRIP_ID.equals(tripId) || docs.trip(tripId).sample;
    }

    public synchronized void setSample(String tripId, boolean sample) {
        docs.trip(tripId).sample = sample;
        docs.saveTrip(tripId);
    }

    /** Member names; a new trip has just "You", the sample trip also has "Alex (sample)". */
    public synchronized List<String> members(String tripId) {
        List<String> saved = docs.trip(tripId).members;
        if (saved != null) {
            return Collections.unmodifiableList(new ArrayList<>(saved));
        }
        return isSample(tripId)
                ? Collections.unmodifiableList(Arrays.asList(SELF_MEMBER, SAMPLE_MEMBER))
                : Collections.singletonList(SELF_MEMBER);
    }

    public synchronized void setMembers(String tripId, List<String> names) {
        docs.trip(tripId).members = new ArrayList<>(names);
        docs.saveTrip(tripId);
    }

    /** Per-day weather labels; empty when none were set. */
    public synchronized List<String> weather(String tripId) {
        return Collections.unmodifiableList(new ArrayList<>(docs.trip(tripId).weather));
    }

    public synchronized void setWeather(String tripId, List<String> perDay) {
        docs.trip(tripId).weather = new ArrayList<>(perDay);
        docs.saveTrip(tripId);
    }

    // ---- budget & expenses ----

    public synchronized BudgetSettings budget(String tripId) {
        TripDoc.BudgetRec rec = docs.trip(tripId).budget;
        BudgetSettings saved = rec == null ? null : TripDocMapper.fromRec(rec);
        TripMeta meta = meta(tripId);
        if (saved != null) {
            return docs.trip(tripId).meta == null ? saved : withTripCurrencies(saved, meta);
        }
        return defaultBudget(meta.getHomeCurrency(), MockData.currencyFor(meta.getCity()));
    }

    /** Saves the budget; the trip's meta stays the single source of the home currency. */
    public synchronized void setBudget(String tripId, BudgetSettings settings) {
        TripDoc doc = docs.trip(tripId);
        doc.budget = TripDocMapper.toRec(settings);
        if (doc.meta != null) {
            doc.meta.homeCurrency = settings.getHomeCurrency();
        }
        docs.saveTrip(tripId);
    }

    /** Home currency from the trip's meta, local currency from its city. */
    private static BudgetSettings withTripCurrencies(BudgetSettings saved, TripMeta meta) {
        return new BudgetSettings(meta.getHomeCurrency(), MockData.currencyFor(meta.getCity()),
                saved.getAmount(), saved.isPerDay(), saved.getCategoryLimits());
    }

    /** Expenses newest-first, re-converted to the trip's current home currency. */
    public synchronized List<Expense> expenses(String tripId) {
        List<TripDoc.ExpenseRec> recs = seededExpenses(tripId);
        String home = budget(tripId).getHomeCurrency();
        List<Expense> converted = new ArrayList<>();
        for (TripDoc.ExpenseRec rec : recs) {
            converted.add(toExpense(rec, home));
        }
        return Collections.unmodifiableList(converted);
    }

    /** Logs an expense entered in the trip's local currency, converting it to home currency. */
    public synchronized Expense addExpense(String tripId, String title, Category category,
                                           double amountLocal, String localCurrency) {
        String home = budget(tripId).getHomeCurrency();
        Expense expense = new Expense(title, category, amountLocal, localCurrency,
                CurrencyRates.convert(amountLocal, localCurrency, home));
        seededExpenses(tripId).add(0, TripDocMapper.toRec(expense));
        docs.saveTrip(tripId);
        return expense;
    }

    private static Expense toExpense(TripDoc.ExpenseRec rec, String home) {
        return new Expense(rec.id != null ? rec.id : UUID.randomUUID().toString(), rec.title,
                TripDocMapper.parseCategory(rec.category), rec.amountLocal, rec.localCurrency,
                CurrencyRates.convert(rec.amountLocal, rec.localCurrency, home));
    }

    /** The trip's live expense records, seeding (and saving) the sample trip on first access. */
    private List<TripDoc.ExpenseRec> seededExpenses(String tripId) {
        TripDoc doc = docs.trip(tripId);
        if (doc.expenses == null) {
            doc.expenses = new ArrayList<>();
            if (TripRepositoryImpl.SEED_TRIP_ID.equals(tripId)) {
                String home = budget(tripId).getHomeCurrency();
                doc.expenses.add(seedExpense("Suica top-up", Category.TRANSPORT, 2000, home));
                doc.expenses.add(seedExpense("teamLab Planets tickets", Category.ACTIVITIES, 3800, home));
                doc.expenses.add(seedExpense("Sushi lunch", Category.FOOD, 8500, home));
                doc.expenses.add(seedExpense("Ramen dinner", Category.FOOD, 3000, home));
            }
            docs.saveTrip(tripId);
        }
        return doc.expenses;
    }

    private static TripDoc.ExpenseRec seedExpense(String title, Category category, double jpy,
                                                  String home) {
        return TripDocMapper.toRec(new Expense(title, category, jpy, "JPY",
                CurrencyRates.convert(jpy, "JPY", home)));
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

    // ---- per-leg transport, per-stop buffers, per-item details ----

    public synchronized TravelMode legMode(String itemId, double km) {
        TravelMode mode = TripDocMapper.parseMode(docs.global().legModes.get(itemId));
        return mode != null ? mode : TravelMode.suggestFor(km);
    }

    public synchronized void setLegMode(String itemId, TravelMode mode) {
        docs.global().legModes.put(itemId, mode.name());
        docs.saveGlobal();
    }

    /** Per-stop buffer minutes, or null when the stop uses the global buffer. */
    @Nullable
    public synchronized Integer stopBuffer(String itemId) {
        return docs.global().stopBuffers.get(itemId);
    }

    public synchronized void setStopBuffer(String itemId, int minutes) {
        docs.global().stopBuffers.put(itemId, minutes);
        docs.saveGlobal();
    }

    /** Cost, currency, category and outdoor flag for an itinerary item, or null if never set. */
    @Nullable
    public synchronized ItemDetails itemDetails(String itemId) {
        return docs.global().itemDetails.get(itemId);
    }

    public synchronized void setItemDetails(String itemId, ItemDetails details) {
        docs.global().itemDetails.put(itemId, details);
        docs.saveGlobal();
    }

    public synchronized void removeItemDetails(String itemId) {
        if (docs.global().itemDetails.remove(itemId) != null) {
            docs.saveGlobal();
        }
    }

    /** Deterministic fake leg distance (km) from an item id, standing in for the Matrix API. */
    public static double legKm(String itemId) {
        return 0.4 + (Math.abs(itemId.hashCode()) % 90) / 10.0;
    }

    // ---- journal ----

    /** Entries newest-first. */
    public synchronized List<JournalEntry> journal(String tripId) {
        List<JournalEntry> entries = new ArrayList<>();
        for (TripDoc.JournalRec rec : seededJournal(tripId)) {
            entries.add(TripDocMapper.fromRec(rec));
        }
        return Collections.unmodifiableList(entries);
    }

    public synchronized void addJournalEntry(String tripId, JournalEntry entry) {
        seededJournal(tripId).add(0, TripDocMapper.toRec(entry));
        docs.saveTrip(tripId);
    }

    private List<TripDoc.JournalRec> seededJournal(String tripId) {
        TripDoc doc = docs.trip(tripId);
        if (doc.journal == null) {
            doc.journal = new ArrayList<>();
            if (TripRepositoryImpl.SEED_TRIP_ID.equals(tripId)) {
                doc.journal.add(TripDocMapper.toRec(new JournalEntry(SEED_JOURNAL_ID, 1,
                        "Senso-ji Temple", null,
                        "Got there early — no crowds. Drew an omikuji fortune: small blessing.",
                        true, null)));
            }
            docs.saveTrip(tripId);
        }
        return doc.journal;
    }

    // ---- group votes (per trip) ----

    /** Demo vote tally: a stable 2-4 base per destination plus the user's own vote. */
    public synchronized int voteCount(String tripId, String destination) {
        int base = 2 + Math.abs(destination.hashCode()) % 3;
        return base + (docs.trip(tripId).votes.contains(destination) ? 1 : 0);
    }

    public synchronized boolean hasVoted(String tripId, String destination) {
        return docs.trip(tripId).votes.contains(destination);
    }

    public synchronized void toggleVote(String tripId, String destination) {
        List<String> votes = docs.trip(tripId).votes;
        if (!votes.remove(destination)) {
            votes.add(destination);
        }
        docs.saveTrip(tripId);
    }

    /** Legacy trip-less form; scoped to {@link #DEFAULT_VOTE_SCOPE}. */
    public int voteCount(String destination) {
        return voteCount(DEFAULT_VOTE_SCOPE, destination);
    }

    /** Legacy trip-less form; scoped to {@link #DEFAULT_VOTE_SCOPE}. */
    public boolean hasVoted(String destination) {
        return hasVoted(DEFAULT_VOTE_SCOPE, destination);
    }

    /** Legacy trip-less form; scoped to {@link #DEFAULT_VOTE_SCOPE}. */
    public void toggleVote(String destination) {
        toggleVote(DEFAULT_VOTE_SCOPE, destination);
    }

    // ---- calendar sync ----

    public synchronized boolean isSynced(String tripId) {
        return docs.trip(tripId).synced;
    }

    public synchronized void markSynced(String tripId) {
        docs.trip(tripId).synced = true;
        docs.saveTrip(tripId);
    }
}
