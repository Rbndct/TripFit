package com.itismob.s01.group01.tripfit.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.itismob.s01.group01.tripfit.data.remote.dto.RatesDto;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.LongSupplier;

/**
 * Keeps {@link CurrencyRates} fresh from Frankfurter. The last good snapshot is cached on the
 * device so an offline start still has recent rates and the full currency list; if a refresh
 * fails, the previous table (cached or built-in) simply stays in force.
 */
public class CurrencyRepository {

    /** Where the last good snapshot is kept; SharedPreferences in the app, memory in tests. */
    public interface Store {
        @Nullable
        String read();

        void write(String json);
    }

    /** ECB publishes once per working day, so refreshing more than twice a day gains nothing. */
    static final long STALE_AFTER_MS = 12L * 60L * 60L * 1000L;

    /** Its own thread: the shared diskIO executor also serves the database, and a slow request would stall it. */
    private static final Executor NETWORK = Executors.newSingleThreadExecutor();

    private final FrankfurterDataSource source;
    private final Store store;
    private final LongSupplier clock;
    private final Gson gson = new Gson();

    public CurrencyRepository(FrankfurterDataSource source, Store store, LongSupplier clock) {
        this.source = source;
        this.store = store;
        this.clock = clock;
    }

    public static CurrencyRepository create(Context context) {
        return new CurrencyRepository(new FrankfurterDataSource(),
                new PrefsStore(context.getApplicationContext()), System::currentTimeMillis);
    }

    /** Restores the cached snapshot, if there is a usable one; returns whether it did. */
    public boolean loadCached() {
        CachedRates cached = readCache();
        if (cached == null) {
            return false;
        }
        CurrencyRates.update(CurrencyRates.Snapshot.of(cached.perUsd, cached.names, cached.asOf, true));
        return true;
    }

    /** True when there is no usable cache or it is older than {@link #STALE_AFTER_MS}. */
    public boolean isStale() {
        CachedRates cached = readCache();
        return cached == null || clock.getAsLong() - cached.fetchedAtMs >= STALE_AFTER_MS;
    }

    /** Refreshes in the background only when the cache is missing or old. */
    public void refreshIfStale() {
        if (isStale()) {
            refreshAsync(null);
        }
    }

    /** Fetches off the main thread; {@code callback}, if given, is delivered on the main thread. */
    public void refreshAsync(@Nullable Result.Callback<CurrencyRates.Snapshot> callback) {
        NETWORK.execute(() -> {
            Result<CurrencyRates.Snapshot> result = refreshSync();
            if (callback != null) {
                AppExecutors.getInstance().mainThread().execute(() -> callback.onResult(result));
            }
        });
    }

    /** Blocking fetch: on success the live table and the cache are updated, on failure nothing changes. */
    public Result<CurrencyRates.Snapshot> refreshSync() {
        try {
            Map<String, String> names = source.fetchCurrenciesSync();
            CurrencyRates.Snapshot snapshot = buildSnapshot(names, source.fetchUsdRatesSync());
            CurrencyRates.update(snapshot);
            store.write(gson.toJson(new CachedRates(clock.getAsLong(), snapshot)));
            return Result.success(snapshot);
        } catch (IOException | RuntimeException e) {
            return Result.error("Could not refresh exchange rates: " + e.getMessage());
        }
    }

    private static CurrencyRates.Snapshot buildSnapshot(Map<String, String> names, RatesDto usd) {
        Map<String, Double> perUsd = new HashMap<>();
        for (Map.Entry<String, Double> entry : usd.getRates().entrySet()) {
            Double rate = entry.getValue();
            if (rate != null && rate > 0) {
                perUsd.put(entry.getKey(), rate);
            }
        }
        perUsd.put(FrankfurterDataSource.BASE_CURRENCY, 1.0); // the API omits the base itself
        return CurrencyRates.Snapshot.of(perUsd, names, usd.getDate(), true);
    }

    @Nullable
    private CachedRates readCache() {
        String json = store.read();
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            CachedRates cached = gson.fromJson(json, CachedRates.class);
            return cached != null && cached.perUsd != null && !cached.perUsd.isEmpty() ? cached : null;
        } catch (RuntimeException e) {
            return null; // a corrupt cache is treated as no cache; the next refresh rewrites it
        }
    }

    /** JSON shape of the cache; annotated so the keys survive minification. */
    private static final class CachedRates {
        @SerializedName("fetchedAtMs")
        long fetchedAtMs;
        @SerializedName("asOf")
        String asOf;
        @SerializedName("perUsd")
        Map<String, Double> perUsd;
        @SerializedName("names")
        Map<String, String> names;

        CachedRates(long fetchedAtMs, CurrencyRates.Snapshot snapshot) {
            this.fetchedAtMs = fetchedAtMs;
            this.asOf = snapshot.asOf();
            this.perUsd = new HashMap<>(snapshot.perUsd());
            this.names = new HashMap<>(snapshot.names());
        }
    }

    private static final class PrefsStore implements Store {
        private static final String FILE = "currency_rates";
        private static final String KEY = "snapshot";
        private final SharedPreferences prefs;

        PrefsStore(Context context) {
            prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        }

        @Nullable
        @Override
        public String read() {
            return prefs.getString(KEY, null);
        }

        @Override
        public void write(String json) {
            prefs.edit().putString(KEY, json).apply();
        }
    }
}
