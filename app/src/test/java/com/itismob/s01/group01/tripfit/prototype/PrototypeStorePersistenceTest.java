package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrototypeStorePersistenceTest {

    private InMemoryKeyValueStorage storage;
    private PrototypeStore store;

    @Before
    public void setUp() {
        storage = new InMemoryKeyValueStorage();
        store = PrototypeStore.forStorage(storage);
    }

    private PrototypeStore reload() {
        return PrototypeStore.forStorage(storage);
    }

    @Test
    public void meta_roundTripsIncludingIsoStartDate() {
        store.setMeta("t1", new TripMeta("Manila", "Jan 5 - Jan 9", 5, "USD", "2027-01-05"));

        TripMeta loaded = reload().meta("t1");

        assertEquals("Manila", loaded.getCity());
        assertEquals("Jan 5 - Jan 9", loaded.getDates());
        assertEquals(5, loaded.getDays());
        assertEquals("USD", loaded.getHomeCurrency());
        assertEquals("2027-01-05", loaded.getStartDateIso());
    }

    @Test
    public void meta_startDateSetterWorks() {
        TripMeta meta = new TripMeta("Tokyo", "", 2, "PHP");
        assertEquals("", meta.getStartDateIso());
        meta.setStartDateIso("2027-02-01");
        assertEquals("2027-02-01", meta.getStartDateIso());
    }

    @Test
    public void unknownTrip_returnsDefaults() {
        PrototypeStore fresh = reload();

        TripMeta meta = fresh.meta("nope");

        assertEquals("Tokyo", meta.getCity());
        assertEquals(2, meta.getDays());
        assertTrue(fresh.expenses("nope").isEmpty());
        assertTrue(fresh.journal("nope").isEmpty());
        assertFalse(fresh.isSynced("nope"));
        assertFalse(fresh.isSample("nope"));
        assertTrue(fresh.weather("nope").isEmpty());
        assertEquals(Arrays.asList("You"), fresh.members("nope"));
    }

    @Test
    public void budget_roundTrips() {
        Map<Category, Double> limits = new EnumMap<>(Category.class);
        limits.put(Category.FOOD, 1234.0);
        limits.put(Category.TRANSPORT, 99.5);
        store.setBudget("t1", new BudgetSettings("EUR", "JPY", 777.0, true, limits));

        BudgetSettings loaded = reload().budget("t1");

        assertEquals("EUR", loaded.getHomeCurrency());
        assertEquals("JPY", loaded.getLocalCurrency());
        assertEquals(777.0, loaded.getAmount(), 0.0001);
        assertTrue(loaded.isPerDay());
        assertEquals(1234.0, loaded.limitFor(Category.FOOD), 0.0001);
        assertEquals(99.5, loaded.limitFor(Category.TRANSPORT), 0.0001);
        assertEquals(0.0, loaded.limitFor(Category.ACTIVITIES), 0.0001);
    }

    @Test
    public void expenses_roundTripWithOriginalCurrencyAndStableIds() {
        Expense first = store.addExpense("t1", "Ramen", Category.FOOD, 1200, "JPY");
        Expense second = store.addExpense("t1", "Taxi", Category.TRANSPORT, 30, "USD");

        List<Expense> loaded = reload().expenses("t1");

        assertEquals(2, loaded.size());
        assertEquals("Taxi", loaded.get(0).getTitle());
        assertEquals(Category.TRANSPORT, loaded.get(0).getCategory());
        assertEquals("USD", loaded.get(0).getLocalCurrency());
        assertEquals(30.0, loaded.get(0).getAmountLocal(), 0.0001);
        assertEquals("Ramen", loaded.get(1).getTitle());
        assertEquals("JPY", loaded.get(1).getLocalCurrency());
        assertEquals(second.getId(), loaded.get(0).getId());
        assertEquals(first.getId(), loaded.get(1).getId());
    }

    @Test
    public void expenseIds_areUniqueAndNonEmpty() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            Expense e = store.addExpense("t1", "e" + i, Category.OTHER, 1, "PHP");
            assertNotNull(e.getId());
            assertFalse(e.getId().isEmpty());
            ids.add(e.getId());
        }
        assertEquals(50, ids.size());
        assertEquals(50, new HashSet<>(idsOf(reload().expenses("t1"))).size());
    }

    private static List<String> idsOf(List<Expense> expenses) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (Expense e : expenses) {
            ids.add(e.getId());
        }
        return ids;
    }

    @Test
    public void sampleTrip_seedsExpensesOnceAndKeepsTheirIds() {
        List<Expense> first = store.expenses(TripRepositoryImpl.SEED_TRIP_ID);
        assertEquals(4, first.size());

        List<Expense> again = reload().expenses(TripRepositoryImpl.SEED_TRIP_ID);

        assertEquals(idsOf(first), idsOf(again));
    }

    @Test
    public void expenses_arePerTrip() {
        store.addExpense("a", "A-thing", Category.FOOD, 1, "PHP");
        store.addExpense("b", "B-thing", Category.FOOD, 2, "PHP");

        PrototypeStore fresh = reload();

        assertEquals(1, fresh.expenses("a").size());
        assertEquals("A-thing", fresh.expenses("a").get(0).getTitle());
        assertEquals("B-thing", fresh.expenses("b").get(0).getTitle());
    }

    @Test
    public void bufferAndHomeCurrencyGlobals_survive() {
        store.setBufferMin(40);
        store.setDefaultHomeCurrency("EUR");

        PrototypeStore fresh = reload();

        assertEquals(40, fresh.getBufferMin());
        assertEquals("EUR", fresh.getDefaultHomeCurrency());
    }

    @Test
    public void globalDefaults_whenNothingSaved() {
        assertEquals(15, store.getBufferMin());
        assertEquals("PHP", store.getDefaultHomeCurrency());
    }

    @Test
    public void journal_roundTripsIdStopIdAndPhotoUri() {
        store.addJournalEntry("t1", new JournalEntry("j-1", 2, "Senso-ji", "stop-9", "Nice", "content://p/1"));
        store.addJournalEntry("t1", new JournalEntry(1, "Plain", "no photo", false));

        List<JournalEntry> loaded = reload().journal("t1");

        assertEquals(2, loaded.size());
        assertEquals("Plain", loaded.get(0).getStopName());
        assertFalse(loaded.get(0).hasPhoto());
        assertNull(loaded.get(0).getPhotoUri());
        assertNull(loaded.get(0).getStopId());
        assertNotNull(loaded.get(0).getId());
        JournalEntry photo = loaded.get(1);
        assertEquals("j-1", photo.getId());
        assertEquals("stop-9", photo.getStopId());
        assertEquals("content://p/1", photo.getPhotoUri());
        assertTrue(photo.hasPhoto());
        assertEquals(2, photo.getDay());
        assertEquals("Nice", photo.getNote());
    }

    @Test
    public void journalSeed_isStableAcrossReload() {
        List<JournalEntry> first = store.journal(TripRepositoryImpl.SEED_TRIP_ID);
        List<JournalEntry> again = reload().journal(TripRepositoryImpl.SEED_TRIP_ID);

        assertEquals(1, first.size());
        assertEquals(first.get(0).getId(), again.get(0).getId());
    }

    @Test
    public void votes_arePerTripAndPersist() {
        store.toggleVote("a", "Kyoto");

        PrototypeStore fresh = reload();

        assertTrue(fresh.hasVoted("a", "Kyoto"));
        assertFalse(fresh.hasVoted("b", "Kyoto"));
        assertEquals(store.voteCount("b", "Kyoto") + 1, fresh.voteCount("a", "Kyoto"));
        fresh.toggleVote("a", "Kyoto");
        assertFalse(reload().hasVoted("a", "Kyoto"));
    }

    @Test
    public void legacyVoteApi_usesDefaultScopeAndPersists() {
        store.toggleVote("Osaka");

        assertTrue(reload().hasVoted("Osaka"));
        assertFalse(reload().hasVoted("t1", "Osaka"));
    }

    @Test
    public void members_defaultsAndPerTrip() {
        assertEquals(Arrays.asList("You"), store.members("new"));
        assertEquals(Arrays.asList("You", "Alex (sample)"),
                store.members(TripRepositoryImpl.SEED_TRIP_ID));

        store.setMembers("new", Arrays.asList("You", "Sam"));

        PrototypeStore fresh = reload();
        assertEquals(Arrays.asList("You", "Sam"), fresh.members("new"));
        assertEquals(Arrays.asList("You"), fresh.members("other"));
    }

    @Test
    public void weather_syncAndSampleFlags_roundTrip() {
        store.setWeather("t1", Arrays.asList("Sunny", "Rain"));
        store.markSynced("t1");
        store.setSample("t1", true);

        PrototypeStore fresh = reload();

        assertEquals(Arrays.asList("Sunny", "Rain"), fresh.weather("t1"));
        assertTrue(fresh.isSynced("t1"));
        assertFalse(fresh.isSynced("t2"));
        assertTrue(fresh.isSample("t1"));
        assertTrue(fresh.isSample(TripRepositoryImpl.SEED_TRIP_ID));
    }

    @Test
    public void itemDetails_roundTripAndRemove() {
        store.setItemDetails("item-1", new ItemDetails(1500.5, "JPY", "Food", true));

        ItemDetails loaded = reload().itemDetails("item-1");

        assertNotNull(loaded);
        assertEquals(1500.5, loaded.getCost(), 0.0001);
        assertEquals("JPY", loaded.getCostCurrency());
        assertEquals("Food", loaded.getCategory());
        assertTrue(loaded.isOutdoor());

        store.removeItemDetails("item-1");
        assertNull(store.itemDetails("item-1"));
        assertNull(reload().itemDetails("item-1"));
    }

    @Test
    public void corruptDocument_isIgnoredAndOtherTripsSurvive() {
        store.addExpense("good", "Keep me", Category.FOOD, 5, "PHP");
        store.addExpense("bad", "Lose me", Category.FOOD, 5, "PHP");
        storage.putString("trip.bad", "{not json[[[");

        PrototypeStore fresh = reload();

        assertEquals("Keep me", fresh.expenses("good").get(0).getTitle());
        assertTrue(fresh.expenses("bad").isEmpty());
        assertEquals("Tokyo", fresh.meta("bad").getCity());
        assertEquals(Arrays.asList("You"), fresh.members("bad"));
    }

    @Test
    public void corruptOrNullDocuments_doNotCrash() {
        storage.putString("trip.x", "null");
        storage.putString("trip.y", "");
        storage.putString("trip.z", "[1,2,3]");
        storage.putString("global", "garbage");

        PrototypeStore fresh = reload();

        assertTrue(fresh.expenses("x").isEmpty());
        assertTrue(fresh.expenses("y").isEmpty());
        assertTrue(fresh.expenses("z").isEmpty());
        assertEquals(15, fresh.getBufferMin());
        assertEquals("PHP", fresh.getDefaultHomeCurrency());
    }

    @Test
    public void unknownEnumValues_fallBackSafely() {
        storage.putString("trip.t", "{\"expenses\":[{\"id\":\"e1\",\"title\":\"x\","
                + "\"category\":\"WAT\",\"localCurrency\":\"PHP\",\"amountLocal\":3.0}]}");

        PrototypeStore fresh = reload();

        assertEquals(Category.OTHER, fresh.expenses("t").get(0).getCategory());
    }

    @Test
    public void returnedLists_areUnmodifiable() {
        store.addExpense("t1", "x", Category.FOOD, 1, "PHP");
        try {
            store.expenses("t1").clear();
            org.junit.Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        assertEquals(1, store.expenses("t1").size());
    }

    @Test
    public void largeData_roundTrips() {
        for (int i = 0; i < 2000; i++) {
            store.addExpense("big", "item " + i + " éñ 😀 ' \"", Category.OTHER, i, "PHP");
        }
        List<Expense> loaded = reload().expenses("big");
        assertEquals(2000, loaded.size());
        assertEquals("item 1999 éñ 😀 ' \"", loaded.get(0).getTitle());
    }

    @Test
    public void getWithoutInit_fallsBackToInMemoryStore() {
        PrototypeStore any = PrototypeStore.get();
        assertNotNull(any);
        assertNotEquals(null, any.meta("whatever"));
    }

    @Test
    public void corruptSeedTrip_isQuarantinedBeforeReseed() {
        String key = "trip." + TripRepositoryImpl.SEED_TRIP_ID;
        storage.putString(key, "{bad seed");

        PrototypeStore fresh = reload();

        assertEquals(4, fresh.expenses(TripRepositoryImpl.SEED_TRIP_ID).size());
        assertEquals("{bad seed", storage.getString(key + ".corrupt"));
    }

    @Test
    public void corruptTrip_rawTextSurvivesLaterWrite() {
        storage.putString("trip.bad", "{oops[");

        PrototypeStore fresh = reload();
        fresh.addExpense("bad", "new", Category.FOOD, 1, "PHP");

        assertEquals("{oops[", storage.getString("trip.bad.corrupt"));
        assertEquals(1, reload().expenses("bad").size());
    }

    @Test
    public void quarantine_doesNotOverwriteExistingCorruptCopy() {
        storage.putString("trip.bad.corrupt", "first");
        storage.putString("trip.bad", "second{");

        reload().expenses("bad");

        assertEquals("first", storage.getString("trip.bad.corrupt"));
    }

    @Test
    public void corruptGlobal_isQuarantinedAndDefaultsApply() {
        store.setItemDetails("i", new ItemDetails(1, "JPY", "Food", false));
        String raw = storage.getString("global");
        String broken = raw.substring(0, raw.length() - 5);
        storage.putString("global", broken);

        PrototypeStore fresh = reload();

        assertEquals(15, fresh.getBufferMin());
        assertNull(fresh.itemDetails("i"));
        assertEquals(broken, storage.getString("global.corrupt"));
    }

    @Test
    public void nullListElements_areStripped() {
        storage.putString("trip.n", "{\"expenses\":[null,{\"id\":\"e1\",\"title\":\"ok\","
                + "\"category\":\"FOOD\",\"localCurrency\":\"PHP\",\"amountLocal\":1.0}],"
                + "\"journal\":[null,{\"id\":\"j1\",\"day\":1,\"note\":\"n\"}],"
                + "\"votes\":[null,\"Kyoto\"],\"members\":[null,\"You\"],\"weather\":[null]}");

        PrototypeStore fresh = reload();

        assertEquals(1, fresh.expenses("n").size());
        assertEquals("ok", fresh.expenses("n").get(0).getTitle());
        assertEquals(1, fresh.journal("n").size());
        assertTrue(fresh.hasVoted("n", "Kyoto"));
        assertEquals(Arrays.asList("You"), fresh.members("n"));
        assertTrue(fresh.weather("n").isEmpty());
    }

    @Test
    public void unparsableLimitKey_isSkippedNotMappedToOther() {
        storage.putString("trip.l", "{\"budget\":{\"homeCurrency\":\"PHP\",\"localCurrency\":\"JPY\","
                + "\"amount\":10.0,\"limits\":{\"OTHER\":7.0,\"WAT\":99.0}}}");

        BudgetSettings loaded = reload().budget("l");

        assertEquals(7.0, loaded.limitFor(Category.OTHER), 0.0001);
        assertEquals(1, loaded.getCategoryLimits().size());
    }

    @Test
    public void nullExpenseId_isBackfilledOnceAndSaved() {
        storage.putString("trip.i", "{\"expenses\":[{\"title\":\"x\",\"category\":\"FOOD\","
                + "\"localCurrency\":\"PHP\",\"amountLocal\":1.0}]}");

        PrototypeStore fresh = reload();
        String first = fresh.expenses("i").get(0).getId();

        assertNotNull(first);
        assertEquals(first, fresh.expenses("i").get(0).getId());
        assertEquals(first, reload().expenses("i").get(0).getId());
    }

    @Test
    public void hasMeta_falseUntilSet() {
        assertFalse(store.hasMeta("fresh"));
        store.setMeta("fresh", new TripMeta("Manila", "", 3, "PHP", "2026-12-01"));
        assertTrue(reload().hasMeta("fresh"));
    }

    @Test
    public void budget_homeCurrencyFollowsTripMeta() {
        store.setMeta("t9", new TripMeta("Tokyo", "", 3, "PHP", "2026-12-01"));
        store.setBudget("t9", new BudgetSettings("PHP", "JPY", 15000, false,
                BudgetMath.defaultLimits(15000)));

        store.setMeta("t9", new TripMeta("Manila", "", 3, "USD", "2026-12-01"));

        BudgetSettings loaded = reload().budget("t9");
        assertEquals("USD", loaded.getHomeCurrency());
        assertEquals("PHP", loaded.getLocalCurrency());
        assertEquals(15000.0, loaded.getAmount(), 0.0);
    }

    @Test
    public void setBudget_syncsHomeCurrencyBackIntoMeta() {
        store.setMeta("t8", new TripMeta("Tokyo", "", 3, "PHP", "2026-12-01"));

        store.setBudget("t8", new BudgetSettings("EUR", "JPY", 500, true,
                BudgetMath.defaultLimits(500)));

        assertEquals("EUR", reload().meta("t8").getHomeCurrency());
    }
    @Test
    public void companionEditsKeepTheOrganizerAndRejectDuplicateNames() {
        store.setMembers("t1", Arrays.asList(" Ana ", "ANA", "you", ""));
        assertEquals(Arrays.asList("You", "Ana"), store.members("t1"));
        assertFalse(store.addCompanion("t1", "ana"));
        assertTrue(store.addCompanion("t1", " Marco "));
        assertFalse(store.removeCompanion("t1", "YOU"));
        assertTrue(store.removeCompanion("t1", "Ana"));
        assertEquals(Arrays.asList("You", "Marco"), reload().members("t1"));
        assertEquals(Arrays.asList("You"), reload().members("other"));
    }

    @Test
    public void voteCountsOnlyIncludeYourLocalVote() {
        assertEquals(0, store.voteCount("trip", "Gallery"));
        store.toggleVote("trip", "Gallery");
        assertEquals(1, reload().voteCount("trip", "Gallery"));
        assertEquals(0, reload().voteCount("other", "Gallery"));
    }

    @Test
    public void journalEditsKeepTheIdAndPositionAndCanBeDeleted() {
        JournalEntry first = new JournalEntry("one", 1, "", null, "First", null);
        JournalEntry second = new JournalEntry("two", 2, "Museum", "stop", "", "content://photos/image");
        store.addJournalEntry("trip", first);
        store.addJournalEntry("trip", second);
        assertTrue(store.updateJournalEntry("trip",
                new JournalEntry("one", 2, "", null, "Edited", null)));

        PrototypeStore restored = reload();
        assertEquals("two", restored.journal("trip").get(0).getId());
        assertEquals("Edited", restored.journal("trip").get(1).getNote());
        assertEquals("content://photos/image", restored.journal("trip").get(0).getPhotoUri());
        assertTrue(restored.removeJournalEntry("trip", "two"));
        assertFalse(restored.removeJournalEntry("trip", "two"));
        assertEquals(1, reload().journal("trip").size());
        assertFalse(reload().journal(TripRepositoryImpl.SEED_TRIP_ID).get(0).hasPhoto());
    }
}
