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

    @Query("SELECT * FROM trips ORDER BY name")
    LiveData<List<TripEntity>> observeAll();

    @Query("SELECT * FROM trips WHERE id = :tripId")
    LiveData<TripEntity> observeById(String tripId);

    @Query("SELECT COUNT(*) FROM trips")
    int countSync();
}
