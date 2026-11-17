package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity;

import java.util.List;

@Dao
public interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PlaceEntity place);

    @Query("SELECT * FROM places WHERE id = :id")
    PlaceEntity findByIdSync(String id);

    @Query("SELECT placeId FROM itinerary_items WHERE id = :itemId")
    String placeIdOfItemSync(String itemId);

    @Query("UPDATE itinerary_items SET placeId = :placeId WHERE id = :itemId")
    void pointItemAt(String itemId, String placeId);

    /**
     * Gives the item its own pin place and points it there, atomically. The item's current place
     * (catalog or earlier pin) supplies category, address, price, rating, image and city so a
     * re-pin only moves the coordinates; an item with no place gets the given cityId (may be null).
     */
    @Transaction
    default void pinItem(String itemId, String pinPlaceId, String cityId, String name,
                         double lat, double lng) {
        String currentId = placeIdOfItemSync(itemId);
        PlaceEntity old = currentId == null ? null : findByIdSync(currentId);
        upsert(PlaceEntity.pinned(pinPlaceId, cityId, name, lat, lng, old));
        pointItemAt(itemId, pinPlaceId);
    }

    @Query("SELECT COUNT(*) FROM places WHERE id = :id")
    int countByIdSync(String id);

    @Query("SELECT * FROM places WHERE cityId = :cityId ORDER BY name")
    LiveData<List<PlaceEntity>> observeByCityId(String cityId);
}
