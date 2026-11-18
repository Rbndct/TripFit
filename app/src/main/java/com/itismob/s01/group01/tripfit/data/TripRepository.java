package com.itismob.s01.group01.tripfit.data;

import androidx.lifecycle.LiveData;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripItemCount;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.ItemDetails;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;

import java.util.List;

/** Repository interface the ViewModels depend on; Room (local) + Supabase REST (remote) back it. */
public interface TripRepository {

    String CATALOG_ALREADY_PLANNED = "catalog_already_planned";
    String CATALOG_STOP_UNAVAILABLE = "catalog_stop_unavailable";

    LiveData<List<TripEntity>> observeTrips();

    LiveData<TripEntity> observeTrip(String tripId);

    /** Distinct day count and total stop count per trip, for the Home trip list. */
    LiveData<List<TripItemCount>> observeTripItemCounts();

    LiveData<List<ItineraryItemEntity>> observeItineraryItems(String tripId);

    /** Same rows as {@link #observeItineraryItems}, left-joined with each referenced place's name. */
    LiveData<List<ItineraryItemWithPlace>> observeItineraryItemsWithPlaceNames(String tripId);

    /** One item with its place name and pin; emits null once the item is deleted. */
    LiveData<ItineraryItemWithPlace> observeItemWithPlace(String itemId);

    /** Adds a free-text stop (no database place match) at the end of its day. */
    default void addItineraryItem(String tripId, String customName, int day, String time,
                                  Integer durationMin, boolean booked,
                                  Result.Callback<Void> callback) {
        addItineraryItem(java.util.UUID.randomUUID().toString(), tripId, customName, day, time,
                durationMin, booked, callback);
    }

    /** Same, but the caller picks the new row's id so it can key extras (cost) to it. */
    void addItineraryItem(String itemId, String tripId, String customName, int day, String time,
                          Integer durationMin, boolean booked, Result.Callback<Void> callback);

    /** Adds a stop backed by a searched-and-selected {@link PlaceDto}, caching it locally so its
     * name renders without a network round trip on every itinerary load. */
    default void addItineraryItemFromPlace(String tripId, PlaceDto place, int day, String time,
                                           Integer durationMin, boolean booked,
                                           Result.Callback<Void> callback) {
        addItineraryItemFromPlace(java.util.UUID.randomUUID().toString(), tripId, place, day, time,
                durationMin, booked, callback);
    }

    /** Same, but the caller picks the new row's id so it can key extras (cost) to it. */
    void addItineraryItemFromPlace(String itemId, String tripId, PlaceDto place, int day,
                                   String time, Integer durationMin, boolean booked,
                                   Result.Callback<Void> callback);

    /** Adds checked sample places, optionally spreading them from the target day. */
    void addCatalogPlaces(String tripId, List<DemoCatalog.Place> places, int targetDay,
                          boolean spread, Result.Callback<Void> callback);

    /** Saves a sample place with the user's validated form values. */
    void addCatalogPlace(String itemId, String tripId, DemoCatalog.Place place, int day,
                         String time, int durationMin, boolean booked, ItemDetails details,
                         Result.Callback<Void> callback);

    /** Replaces one flexible stop in place, preserving its incoming leg settings. */
    void replaceWithCatalogPlace(String tripId, String itemId, DemoCatalog.Place place,
                                 Result.Callback<Void> callback);

    /** Exchanges a flexible outdoor stop's slot with a flexible indoor stop on another day. */
    void swapWeatherStops(String tripId, String outdoorId, String indoorId,
                          Result.Callback<Void> callback);

    /** Creates a new trip and hands back its id so the caller can navigate into it. */
    void createTrip(String name, Result.Callback<String> callback);

    /** Renames a trip with an UPDATE, so its stops survive (an insert would REPLACE and cascade). */
    void renameTrip(String tripId, String name, Result.Callback<Void> callback);

    /** The last day that has a stop, or 0 when the trip has none. */
    void maxStopDay(String tripId, Result.Callback<Integer> callback);

    /** First-run only: inserts the sample Tokyo trip + stops if no trip exists yet. */
    void ensureSeedData(Result.Callback<Void> callback);

    /** Lists the pipeline-seeded cities (currently Tokyo, Manila) for the destination search's city picker. */
    void fetchCities(Result.Callback<List<CityDto>> callback);

    /** Searches the given city's places by name substring (case-insensitive). */
    void searchPlaces(String cityId, String query, Result.Callback<List<PlaceDto>> callback);

    /** Edits a stop including buffer and transport mode in one UPDATE (null buffer = profile default,
     * null mode = Suggested). Moving to another day appends it to the end of that day. */
    void updateItineraryItem(String tripId, String itemId, int oldDay, int oldPosition, int newDay,
                             String time, Integer durationMin, boolean booked, Integer bufferMin,
                             String transportMode, Result.Callback<Void> callback);

    /** Saves a leg's buffer and mode in one UPDATE ({@code modeOrNull} null = Suggested). */
    void updateLegSettings(String itemId, Integer bufferMin, String modeOrNull,
                           Result.Callback<Void> callback);

    void updateBuffer(String itemId, Integer bufferMin, Result.Callback<Void> callback);

    /** {@code modeOrNull} is a TravelMode name; null means Suggested. */
    void updateTransportMode(String itemId, String modeOrNull, Result.Callback<Void> callback);

    /**
     * Gives one item its own pin: creates place {@code "pin-" + itemId} (never a shared place, so
     * other items keep their pin) and points the item at it. {@code cityId} is null or the trip's city.
     * Fails for lat outside -90..90 or lng outside -180..180.
     */
    void adjustPin(String itemId, String cityId, String name, double lat, double lng,
                   Result.Callback<Void> callback);

    /**
     * {@link #updateItineraryItem} plus {@link #adjustPin} in one transaction: either both land or
     * neither does. The item's existing place details are kept; only the coordinates move.
     */
    void updateItineraryItemWithPin(String tripId, String itemId, int oldDay, int oldPosition,
                                    int newDay, String time, Integer durationMin, boolean booked,
                                    Integer bufferMin, String transportMode, String pinName,
                                    double pinLat, double pinLng, Result.Callback<Void> callback);

    /** Applies day and position changes for many items in one transaction. */
    void applyMoves(List<TripSnapshots.ItemMove> moves, Result.Callback<Void> callback);

    /** Rewrites positions for several days in one transaction; each inner list is one day's order. */
    void applyItemOrders(List<List<String>> orderedItemIdsPerDay, Result.Callback<Void> callback);

    void deleteItineraryItem(String itemId, Result.Callback<Void> callback);
}
