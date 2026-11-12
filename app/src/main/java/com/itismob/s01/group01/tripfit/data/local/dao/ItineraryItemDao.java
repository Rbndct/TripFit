package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripItemCount;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import java.util.List;

@Dao
public interface ItineraryItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ItineraryItemEntity item);

    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId ORDER BY day, position")
    LiveData<List<ItineraryItemEntity>> observeByTripId(String tripId);

    @Query("SELECT tripId, COUNT(DISTINCT day) AS dayCount, COUNT(*) AS stopCount "
            + "FROM itinerary_items GROUP BY tripId")
    LiveData<List<TripItemCount>> observeItemCounts();

    @Query("SELECT i.*, p.name AS placeName FROM itinerary_items i "
            + "LEFT JOIN places p ON p.id = i.placeId "
            + "WHERE i.tripId = :tripId ORDER BY i.day, i.position")
    LiveData<List<ItineraryItemWithPlace>> observeByTripIdWithPlace(String tripId);

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM itinerary_items WHERE tripId = :tripId AND day = :day")
    int nextPositionSync(String tripId, int day);

    @Query("UPDATE itinerary_items SET day = :day, time = :time, durationMin = :durationMin, "
            + "isBooked = :booked, position = :position WHERE id = :id")
    void updateDetails(String id, int day, String time, Integer durationMin, boolean booked,
                       int position);

    @Query("UPDATE itinerary_items SET position = :position WHERE id = :id")
    void updatePosition(String id, int position);

    @Query("DELETE FROM itinerary_items WHERE id = :id")
    void deleteById(String id);
}
