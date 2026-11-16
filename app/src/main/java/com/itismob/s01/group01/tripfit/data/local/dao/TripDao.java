package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;

import java.util.List;

@Dao
public interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TripEntity trip);

    /**
     * Newest first. Trips have no creation timestamp, so insertion order (rowid) stands in; that
     * only holds while trips are never REPLACE-inserted (a REPLACE assigns a new rowid).
     */
    @Query("SELECT * FROM trips ORDER BY rowid DESC")
    LiveData<List<TripEntity>> observeAll();

    @Query("SELECT * FROM trips WHERE id = :tripId")
    LiveData<TripEntity> observeById(String tripId);

    @Query("SELECT COUNT(*) FROM trips")
    int countSync();

    @Query("SELECT COUNT(*) FROM trips WHERE id = :tripId")
    int countByIdSync(String tripId);

    /** Renames a trip in place. Never edit a trip through {@link #insert}: REPLACE deletes its stops. */
    @Query("UPDATE trips SET name = :name WHERE id = :tripId")
    void updateName(String tripId, String name);
}
