package com.itismob.s01.group01.tripfit.prototype;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Non-persistent storage for unit tests and for launches where the store was never initialised. */
public final class InMemoryKeyValueStorage implements KeyValueStorage {

    private final Map<String, String> values = new HashMap<>();

    @Nullable
    @Override
    public synchronized String getString(String key) {
        return values.get(key);
    }

    @Override
    public synchronized void putString(String key, String value) {
        values.put(key, value);
    }
}
