package com.itismob.s01.group01.tripfit.data;

import androidx.lifecycle.LiveData;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripItemCount;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import java.util.List;

/** Repository interface the ViewModels depend on; Room (local) + Supabase REST (remote) back it. */
public interface TripRepository {

    LiveData<List<TripEntity>> observeTrips();

    LiveData<TripEntity> observeTrip(String tripId);

    /** Distinct day count and total stop count per trip, for the Home trip list. */
    LiveData<List<TripItemCount>> observeTripItemCounts();

    LiveData<List<ItineraryItemEntity>> observeItineraryItems(String tripId);

    /** Same rows as {@link #observeItineraryItems}, left-joined with each referenced place's name. */
    LiveData<List<ItineraryItemWithPlace>> observeItineraryItemsWithPlaceNames(String tripId);

    /** Adds a free-text stop (no database place match) at the end of its day. */
    void addItineraryItem(String tripId, String customName, int day, String time,
                           Integer durationMin, boolean booked, Result.Callback<Void> callback);

    /** Adds a stop backed by a searched-and-selected {@link PlaceDto}, caching it locally so its
     * name renders without a network round trip on every itinerary load. */
    void addItineraryItemFromPlace(String tripId, PlaceDto place, int day, String time,
                                    Integer durationMin, boolean booked,
                                    Result.Callback<Void> callback);

    /** Creates a new trip and hands back its id so the caller can navigate into it. */
    void createTrip(String name, Result.Callback<String> callback);

    /** First-run only: inserts the sample Tokyo trip + stops if no trip exists yet. */
    void ensureSeedData(Result.Callback<Void> callback);

    /** Lists the pipeline-seeded cities (currently Tokyo, Manila) for the destination search's city picker. */
    void fetchCities(Result.Callback<List<CityDto>> callback);

    /** Searches the given city's places by name substring (case-insensitive). */
    void searchPlaces(String cityId, String query, Result.Callback<List<PlaceDto>> callback);

    /** Edits a stop; moving it to another day appends it to the end of that day. */
    void updateItineraryItem(String tripId, String itemId, int oldDay, int oldPosition, int newDay,
                             String time, Integer durationMin, boolean booked,
                             Result.Callback<Void> callback);

    void deleteItineraryItem(String itemId, Result.Callback<Void> callback);

    /** Rewrites positions so the given item ids appear in exactly this order within their day. */
    void applyItemOrder(List<String> orderedItemIds, Result.Callback<Void> callback);
}
