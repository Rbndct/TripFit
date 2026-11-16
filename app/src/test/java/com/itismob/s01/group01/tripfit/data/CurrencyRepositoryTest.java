package com.itismob.s01.group01.tripfit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class CurrencyRepositoryTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long HOUR_MS = 60L * 60L * 1000L;

    /** In-memory stand-in for the SharedPreferences-backed store. */
    private static final class MemoryStore implements CurrencyRepository.Store {
        String json;
        int writes;

        @Override
        public String read() {
            return json;
        }

        @Override
        public void write(String value) {
            json = value;
            writes++;
        }
    }

    @After
    public void restoreFallback() {
        CurrencyRates.useFallback();
    }

    private CurrencyRepository repository(FakeFrankfurterClient fake, MemoryStore store, long now) {
        return new CurrencyRepository(new FrankfurterDataSource(fake.client()), store, () -> now);
    }

    @Test
    public void refreshSync_success_updatesTheLiveRatesAndWritesTheCache() {
        MemoryStore store = new MemoryStore();

        Result<CurrencyRates.Snapshot> result =
                repository(new FakeFrankfurterClient(), store, NOW).refreshSync();

        assertTrue(result.isSuccess());
        assertTrue(CurrencyRates.isLive());
        assertEquals("2026-11-13", CurrencyRates.asOfDate());
        assertEquals(64.0, CurrencyRates.convert(1, "USD", "PHP"), 0.0001);
        assertEquals("Philippine Peso", CurrencyRates.nameOf("PHP"));
        assertEquals(1, store.writes);
        assertNotNull(store.json);
    }

    @Test
    public void refreshSync_success_includesUsdEvenThoughTheApiOmitsTheBase() {
        repository(new FakeFrankfurterClient(), new MemoryStore(), NOW).refreshSync();

        assertTrue(CurrencyRates.codes().contains("USD"));
        assertEquals(1.0, CurrencyRates.convert(1, "USD", "USD"), 0.0001);
    }

    @Test
    public void refreshSync_failure_keepsTheFallbackAndLeavesTheCacheAlone() {
        MemoryStore store = new MemoryStore();
        store.json = "previous";

        Result<CurrencyRates.Snapshot> result =
                repository(FakeFrankfurterClient.failingWith(500), store, NOW).refreshSync();

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertFalse(CurrencyRates.isLive());
        assertEquals("previous", store.json);
        assertEquals(0, store.writes);
    }

    @Test
    public void loadCached_restoresTheLastGoodSnapshot() {
        MemoryStore store = new MemoryStore();
        repository(new FakeFrankfurterClient(), store, NOW).refreshSync();
        CurrencyRates.useFallback();

        boolean restored = repository(FakeFrankfurterClient.failingWith(500), store, NOW).loadCached();

        assertTrue(restored);
        assertTrue(CurrencyRates.isLive());
        assertEquals("2026-11-13", CurrencyRates.asOfDate());
        assertEquals(64.0, CurrencyRates.convert(1, "USD", "PHP"), 0.0001);
    }

    @Test
    public void loadCached_ignoresMissingOrCorruptCaches() {
        MemoryStore empty = new MemoryStore();
        MemoryStore corrupt = new MemoryStore();
        corrupt.json = "{not json";
        MemoryStore noRates = new MemoryStore();
        noRates.json = "{\"fetchedAtMs\":1}";

        assertFalse(repository(new FakeFrankfurterClient(), empty, NOW).loadCached());
        assertFalse(repository(new FakeFrankfurterClient(), corrupt, NOW).loadCached());
        assertFalse(repository(new FakeFrankfurterClient(), noRates, NOW).loadCached());
        assertFalse(CurrencyRates.isLive());
    }

    @Test
    public void isStale_trueWithoutACache() {
        assertTrue(repository(new FakeFrankfurterClient(), new MemoryStore(), NOW).isStale());
    }

    @Test
    public void isStale_falseWhenFreshAndTrueOnceOlderThanTwelveHours() {
        MemoryStore store = new MemoryStore();
        repository(new FakeFrankfurterClient(), store, NOW).refreshSync();

        assertFalse(repository(new FakeFrankfurterClient(), store, NOW + 11 * HOUR_MS).isStale());
        assertTrue(repository(new FakeFrankfurterClient(), store, NOW + 13 * HOUR_MS).isStale());
    }

    @Test
    public void isStale_trueWhenTheCacheIsCorrupt() {
        MemoryStore store = new MemoryStore();
        store.json = "{not json";

        assertTrue(repository(new FakeFrankfurterClient(), store, NOW).isStale());
    }
}
