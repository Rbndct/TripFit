package com.itismob.s01.group01.tripfit.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Mirrors the per-user `itinerary_items` table (API-Integration-Plan.md §3), replacing the
 * UI-only {@code model.ItineraryItem} POJO.
 *
 * <p>{@code customName} is a deliberate extension beyond the plan's literal column list: Phase 0
 * has no place-search yet (that lands in Phase 2), so a manually-typed stop has no
 * {@code placeId} to reference. Once search exists, new stops will set {@code placeId} and
 * {@code customName} will stay null.
 */
@Entity(
        tableName = "itinerary_items",
        foreignKeys = @ForeignKey(
                entity = TripEntity.class,
                parentColumns = "id",
                childColumns = "tripId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("tripId")}
)
public class ItineraryItemEntity {

    @PrimaryKey
    @NonNull
    private final String id;
    private final String tripId;
    private final String placeId;
    private final String customName;
    private final int day;
    private final String time;
    private final Integer durationMin;
    private final boolean isBooked;
    private final String transportMode;
    private final Integer bufferMin;
    private final int position;

    public ItineraryItemEntity(@NonNull String id, String tripId, String placeId,
                                String customName, int day, String time, Integer durationMin,
                                boolean isBooked, String transportMode, Integer bufferMin,
                                int position) {
        this.id = id;
        this.tripId = tripId;
        this.placeId = placeId;
        this.customName = customName;
        this.day = day;
        this.time = time;
        this.durationMin = durationMin;
        this.isBooked = isBooked;
        this.transportMode = transportMode;
        this.bufferMin = bufferMin;
        this.position = position;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getTripId() {
        return tripId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getCustomName() {
        return customName;
    }

    public int getDay() {
        return day;
    }

    public String getTime() {
        return time;
    }

    public Integer getDurationMin() {
        return durationMin;
    }

    public boolean isBooked() {
        return isBooked;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public Integer getBufferMin() {
        return bufferMin;
    }

    public int getPosition() {
        return position;
    }

    /** Resolves the display name: the referenced place's name once search lands, until then the typed name. */
    @NonNull
    public String displayName(String placeName) {
        return placeName != null ? placeName : (customName != null ? customName : "");
    }
}
