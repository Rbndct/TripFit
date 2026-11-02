package com.itismob.s01.group01.tripfit.data;

import androidx.lifecycle.LiveData;

import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;

import java.util.List;

/** Repository interface the ViewModels depend on; Room is the only implementation for now. */
public interface TripRepository {

    LiveData<List<TripEntity>> observeTrips();

    LiveData<TripEntity> observeTrip(String tripId);

    LiveData<List<ItineraryItemEntity>> observeItineraryItems(String tripId);

    /** Adds a stop at the end of its day, assigning position server-side-style (on the DB thread). */
    void addItineraryItem(String tripId, String customName, int day, String time,
                           Integer durationMin, boolean booked, Result.Callback<Void> callback);

    /** First-run only: inserts the sample Tokyo trip + stops if no trip exists yet. */
    void ensureSeedData(Result.Callback<Void> callback);
}
