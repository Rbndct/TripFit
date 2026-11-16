package com.itismob.s01.group01.tripfit.prototype;

import androidx.annotation.Nullable;

/** Minimal string key/value seam behind {@link PrototypeStore} (SharedPreferences in the app). */
public interface KeyValueStorage {

    @Nullable
    String getString(String key);

    void putString(String key, String value);
}
