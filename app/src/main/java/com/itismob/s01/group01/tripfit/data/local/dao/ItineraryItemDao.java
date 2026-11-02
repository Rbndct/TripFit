package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import java.util.List;

@Dao
public interface ItineraryItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ItineraryItemEntity item);

    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId ORDER BY day, position")
    LiveData<List<ItineraryItemEntity>> observeByTripId(String tripId);

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM itinerary_items WHERE tripId = :tripId AND day = :day")
    int nextPositionSync(String tripId, int day);
}
