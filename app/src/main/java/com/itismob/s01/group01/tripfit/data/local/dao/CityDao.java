package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Upsert;
import androidx.room.Query;

import com.itismob.s01.group01.tripfit.data.local.entity.CityEntity;

import java.util.List;

@Dao
public interface CityDao {

    @Upsert
    void upsert(CityEntity city);

    @Query("SELECT name FROM cities WHERE id = :id")
    String nameByIdSync(String id);

    @Query("SELECT * FROM cities ORDER BY name")
    LiveData<List<CityEntity>> observeAll();
}
