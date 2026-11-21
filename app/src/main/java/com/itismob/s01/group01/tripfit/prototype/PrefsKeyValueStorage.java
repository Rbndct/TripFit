package com.itismob.s01.group01.tripfit.prototype;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * SharedPreferences-backed storage. Uses its own file, never Kevin's {@code trippings_prototype_v1}
 * (that file holds an incompatible {@code TripSnapshot} array).
 */
public final class PrefsKeyValueStorage implements KeyValueStorage {

    public static final String FILE_NAME = "trippings_store_v2";

    private final SharedPreferences preferences;

    public PrefsKeyValueStorage(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public String getString(String key) {
        return preferences.getString(key, null);
    }

    @Override
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }
}
