package com.itismob.s01.group01.tripfit.prototype;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads and saves the JSON documents over a {@link KeyValueStorage}, with a read cache. A missing,
 * corrupt or wrong-shaped document yields a fresh empty one and never throws; each trip is its own
 * document, so one bad trip cannot affect another. Callers synchronise externally.
 */
final class DocumentStore {

    private static final String TRIP_PREFIX = "trip.";
    private static final String GLOBAL_KEY = "global";
    private static final String CORRUPT_SUFFIX = ".corrupt";

    private final KeyValueStorage storage;
    private final Gson gson = new Gson();
    private final Map<String, TripDoc> trips = new HashMap<>();
    private GlobalDoc global;

    DocumentStore(KeyValueStorage storage) {
        this.storage = storage;
    }

    TripDoc trip(String tripId) {
        TripDoc doc = trips.get(tripId);
        if (doc == null) {
            doc = read(TRIP_PREFIX + tripId, TripDoc.class);
            if (doc == null) {
                doc = new TripDoc();
            }
            trips.put(tripId, doc);
            if (normalise(doc)) {
                saveTrip(tripId);
            }
        }
        return doc;
    }

    GlobalDoc global() {
        if (global == null) {
            global = read(GLOBAL_KEY, GlobalDoc.class);
            if (global == null) {
                global = new GlobalDoc();
            }
            normalise(global);
        }
        return global;
    }

    void saveTrip(String tripId) {
        storage.putString(TRIP_PREFIX + tripId, gson.toJson(trip(tripId)));
    }

    void saveGlobal() {
        storage.putString(GLOBAL_KEY, gson.toJson(global()));
    }

    private <T> T read(String key, Class<T> type) {
        String json = storage.getString(key);
        if (json == null || json.isEmpty()) {
            return null;
        }
        T parsed;
        try {
            parsed = gson.fromJson(json, type);
        } catch (RuntimeException e) {
            parsed = null;
        }
        if (parsed == null) {
            quarantine(key, json);
        }
        return parsed;
    }

    /** Keeps the raw text of an unreadable document, never replacing an earlier quarantined copy. */
    private void quarantine(String key, String raw) {
        String copyKey = key + CORRUPT_SUFFIX;
        if (storage.getString(copyKey) == null) {
            storage.putString(copyKey, raw);
        }
    }

    /** Drops null list elements and backfills missing ids. Returns true when ids were added. */
    private static boolean normalise(TripDoc doc) {
        if (doc.votes == null) {
            doc.votes = new ArrayList<>();
        }
        if (doc.weather == null) {
            doc.weather = new ArrayList<>();
        }
        stripNulls(doc.votes);
        stripNulls(doc.weather);
        stripNulls(doc.members);
        stripNulls(doc.expenses);
        stripNulls(doc.journal);
        boolean changed = false;
        if (doc.expenses != null) {
            for (TripDoc.ExpenseRec rec : doc.expenses) {
                if (rec.id == null) {
                    rec.id = UUID.randomUUID().toString();
                    changed = true;
                }
            }
        }
        if (doc.journal != null) {
            for (TripDoc.JournalRec rec : doc.journal) {
                if (rec.id == null) {
                    rec.id = UUID.randomUUID().toString();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static void stripNulls(List<?> list) {
        if (list != null) {
            list.removeAll(Collections.singleton(null));
        }
    }

    /** Gson leaves a map null when the JSON omits it or sets it to null. */
    private static void normalise(GlobalDoc doc) {
        if (doc.legModes == null) {
            doc.legModes = new HashMap<>();
        }
        if (doc.stopBuffers == null) {
            doc.stopBuffers = new HashMap<>();
        }
        if (doc.itemDetails == null) {
            doc.itemDetails = new HashMap<>();
        }
    }
}
