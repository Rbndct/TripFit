package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import java.util.List;

/**
 * Frontend storage boundary. all/get/save access a local cache and never perform network IO.
 * A future backend implementation must save locally first and queue durable, account-scoped
 * writes. refresh performs asynchronous remote reads and delivers its callback on the main
 * thread; a failure must leave the last local drafts intact.
 */
public interface DraftRepository {
    List<DraftTrip> all();
    DraftTrip get(String id);
    void save(DraftTrip trip);
    void refresh(Result.Callback<List<DraftTrip>> callback);
}
