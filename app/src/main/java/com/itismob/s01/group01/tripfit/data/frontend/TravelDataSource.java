package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.data.Result;

/**
 * Read-only API boundary. cached must never perform network IO. refresh loads data
 * asynchronously, updates the cache only after validation, and reports errors without
 * discarding existing data. Retrofit DTOs and credentials belong inside the adapter.
 */
public interface TravelDataSource {
    TravelSnapshot cached(TravelQuery query);
    void refresh(TravelQuery query,Result.Callback<TravelSnapshot> callback);
}
