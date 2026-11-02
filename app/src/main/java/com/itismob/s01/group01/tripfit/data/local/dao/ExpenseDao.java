package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.itismob.s01.group01.tripfit.data.local.entity.ExpenseEntity;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ExpenseEntity expense);

    @Query("SELECT * FROM expenses WHERE tripId = :tripId")
    LiveData<List<ExpenseEntity>> observeByTripId(String tripId);
}
