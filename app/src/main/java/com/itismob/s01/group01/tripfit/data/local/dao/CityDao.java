package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.itismob.s01.group01.tripfit.data.local.entity.CityEntity;

import java.util.List;

@Dao
public interface CityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CityEntity city);

    @Query("SELECT * FROM cities ORDER BY name")
    LiveData<List<CityEntity>> observeAll();
}
