package com.itismob.s01.group01.tripfit.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripItemCount;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;

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

    @Query("SELECT i.*, p.name AS placeName, p.lat AS placeLat, p.lng AS placeLng "
            + "FROM itinerary_items i LEFT JOIN places p ON p.id = i.placeId "
            + "WHERE i.tripId = :tripId ORDER BY i.day, i.position")
    LiveData<List<ItineraryItemWithPlace>> observeByTripIdWithPlace(String tripId);

    @Query("SELECT i.*, p.name AS placeName, p.lat AS placeLat, p.lng AS placeLng "
            + "FROM itinerary_items i LEFT JOIN places p ON p.id = i.placeId "
            + "WHERE i.id = :id")
    LiveData<ItineraryItemWithPlace> observeByIdWithPlace(String id);

    @Query("SELECT COALESCE(MAX(day), 0) FROM itinerary_items WHERE tripId = :tripId")
    int maxDaySync(String tripId);

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM itinerary_items WHERE tripId = :tripId AND day = :day")
    int nextPositionSync(String tripId, int day);

    /** Updates the stop's day, time, stay, booked flag and position with its routing fields in one UPDATE. */
    @Query("UPDATE itinerary_items SET day = :day, time = :time, durationMin = :durationMin, "
            + "isBooked = :booked, position = :position, bufferMin = :bufferMin, "
            + "transportMode = :transportMode WHERE id = :id")
    void updateDetailsWithRouting(String id, int day, String time, Integer durationMin,
                                  boolean booked, int position, Integer bufferMin,
                                  String transportMode);

    /** Writes the incoming leg's buffer and mode together (mode null = Suggested). */
    @Query("UPDATE itinerary_items SET bufferMin = :bufferMin, transportMode = :modeOrNull "
            + "WHERE id = :id")
    void updateLegSettings(String id, Integer bufferMin, String modeOrNull);

    @Query("UPDATE itinerary_items SET bufferMin = :bufferMin WHERE id = :id")
    void updateBuffer(String id, Integer bufferMin);

    /** {@code modeOrNull} is a TravelMode name; null means Suggested. */
    @Query("UPDATE itinerary_items SET transportMode = :modeOrNull WHERE id = :id")
    void updateTransportMode(String id, String modeOrNull);

    @Query("UPDATE itinerary_items SET placeId = :placeId WHERE id = :id")
    void updatePlace(String id, String placeId);

    @Query("UPDATE itinerary_items SET day = :day, position = :position WHERE id = :id")
    void updateDayAndPosition(String id, int day, int position);

    @Transaction
    default void applyMoves(List<TripSnapshots.ItemMove> moves) {
        for (TripSnapshots.ItemMove move : moves) {
            updateDayAndPosition(move.id, move.day, move.position);
        }
    }

    @Transaction
    default void applyOrders(List<List<String>> orders) {
        for (List<String> order : orders) {
            for (int i = 0; i < order.size(); i++) {
                updatePosition(order.get(i), i);
            }
        }
    }

    @Query("UPDATE itinerary_items SET position = :position WHERE id = :id")
    void updatePosition(String id, int position);

    @Query("DELETE FROM itinerary_items WHERE id = :id")
    void deleteById(String id);
}
