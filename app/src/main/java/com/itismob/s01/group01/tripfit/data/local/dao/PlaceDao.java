package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity;

import java.util.List;

@Dao
public interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PlaceEntity place);

    @Query("SELECT * FROM places WHERE cityId = :cityId ORDER BY name")
    LiveData<List<PlaceEntity>> observeByCityId(String cityId);
}
