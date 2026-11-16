package com.itismob.s01.group01.tripfit.data;

import androidx.lifecycle.LiveData;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.TripItemCount;
import com.itismob.s01.group01.tripfit.data.local.entity.CityEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripDates;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;

import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class TripRepositoryImpl implements TripRepository {

    /** Fixed id so the Phase 0 seed trip is idempotent across app restarts. */
    public static final String SEED_TRIP_ID = "seed-trip-tokyo";
    private static final String SEED_START_DATE = "2026-12-01";
    private static final int SEED_DAYS = 2;
    private static final String TAG = "TripRepository";
    private static final int PLACE_SEARCH_LIMIT = 25;

    private final TripFitDatabase database;
    private final AppExecutors executors;
    private final RemoteDataSource remoteDataSource;

    public TripRepositoryImpl(TripFitDatabase database, AppExecutors executors,
                               RemoteDataSource remoteDataSource) {
        this.database = database;
        this.executors = executors;
        this.remoteDataSource = remoteDataSource;
    }

    @Override
    public LiveData<List<TripEntity>> observeTrips() {
        return database.tripDao().observeAll();
    }

    @Override
    public LiveData<TripEntity> observeTrip(String tripId) {
        return database.tripDao().observeById(tripId);
    }

    @Override
    public LiveData<List<TripItemCount>> observeTripItemCounts() {
        return database.itineraryItemDao().observeItemCounts();
    }

    @Override
    public LiveData<List<ItineraryItemEntity>> observeItineraryItems(String tripId) {
        return database.itineraryItemDao().observeByTripId(tripId);
    }

    @Override
    public LiveData<List<ItineraryItemWithPlace>> observeItineraryItemsWithPlaceNames(String tripId) {
        return database.itineraryItemDao().observeByTripIdWithPlace(tripId);
    }

    @Override
    public void addItineraryItem(String tripId, String customName, int day, String time,
                                  Integer durationMin, boolean booked,
                                  Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                int position = database.itineraryItemDao().nextPositionSync(tripId, day);
                ItineraryItemEntity item = new ItineraryItemEntity(
                        UUID.randomUUID().toString(), tripId, null, customName, day, time,
                        durationMin, booked, null, null, position
                );
                database.itineraryItemDao().insert(item);
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not save destination: " + e.getMessage()));
            }
        });
    }

    @Override
    public void addItineraryItemFromPlace(String tripId, PlaceDto place, int day, String time,
                                           Integer durationMin, boolean booked,
                                           Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                database.placeDao().upsert(new PlaceEntity(
                        place.getId(), place.getCityId(), place.getName(), place.getCategory(),
                        place.getLat(), place.getLng(), place.getAddress(), place.getPriceTier(),
                        place.getRating(), place.isHiddenGem(), place.getDescription(),
                        place.getImageUrl(), null, null, null, null
                ));
                int position = database.itineraryItemDao().nextPositionSync(tripId, day);
                ItineraryItemEntity item = new ItineraryItemEntity(
                        UUID.randomUUID().toString(), tripId, place.getId(), null, day, time,
                        durationMin, booked, null, null, position
                );
                database.itineraryItemDao().insert(item);
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not save destination: " + e.getMessage()));
            }
        });
    }

    @Override
    public void createTrip(String name, Result.Callback<String> callback) {
        executors.diskIO().execute(() -> {
            try {
                String tripId = UUID.randomUUID().toString();
                database.tripDao().insert(new TripEntity(tripId, null, name, null, null, null, null));
                deliver(callback, Result.success(tripId));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not create trip: " + e.getMessage()));
            }
        });
    }

    @Override
    public void renameTrip(String tripId, String name, Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                database.tripDao().updateName(tripId, name);
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not rename trip: " + e.getMessage()));
            }
        });
    }

    @Override
    public void maxStopDay(String tripId, Result.Callback<Integer> callback) {
        executors.diskIO().execute(() -> {
            try {
                deliver(callback, Result.success(database.itineraryItemDao().maxDaySync(tripId)));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not read stops: " + e.getMessage()));
            }
        });
    }

    @Override
    public void ensureSeedData(Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                if (database.tripDao().countSync() == 0) {
                    database.tripDao().insert(new TripEntity(
                            SEED_TRIP_ID, null, "Tokyo Trip", null, null, null, null));
                    insertSeedStop("Senso-ji Temple", 1, 0, 45, false, null);
                    insertSeedStop("Nakamise Shopping Street", 1, 1, 30, false, null);
                    insertSeedStop("Tokyo Skytree", 1, 2, null, true, "18:00");
                    insertSeedStop("Meiji Shrine", 2, 0, 40, false, null);
                    insertSeedStop("Shibuya Crossing", 2, 1, 20, false, null);
                    insertSeedStop("teamLab Planets", 2, 2, null, true, "15:30");
                }
                if (database.tripDao().countByIdSync(SEED_TRIP_ID) > 0) {
                    seedStoreState();
                }
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not seed trip data: " + e.getMessage()));
            }
        });
    }

    @Override
    public void fetchCities(Result.Callback<List<CityDto>> callback) {
        executors.diskIO().execute(() -> {
            List<CityDto> cities;
            try {
                cities = remoteDataSource.fetchCitiesSync();
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "Supabase cities unavailable, using offline demo data: " + e.getMessage());
                cities = OfflineData.cities();
            }
            // Cache locally so places selected from search can reference a city that
            // already satisfies PlaceEntity's foreign key.
            for (CityDto city : cities) {
                database.cityDao().upsert(new CityEntity(
                        city.getId(), city.getName(), city.getCountry(), city.getLat(),
                        city.getLng(), null, null, System.currentTimeMillis()
                ));
            }
            deliver(callback, Result.success(cities));
        });
    }

    @Override
    public void searchPlaces(String cityId, String query, Result.Callback<List<PlaceDto>> callback) {
        executors.diskIO().execute(() -> {
            List<PlaceDto> places;
            try {
                places = remoteDataSource.searchPlacesSync(cityId, query, PLACE_SEARCH_LIMIT);
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "Supabase search unavailable, using offline demo data: " + e.getMessage());
                places = OfflineData.search(cityId, query);
            }
            deliver(callback, Result.success(places));
        });
    }

    @Override
    public void updateItineraryItem(String tripId, String itemId, int oldDay, int oldPosition,
                                    int newDay, String time, Integer durationMin, boolean booked,
                                    Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                int position = newDay == oldDay
                        ? oldPosition
                        : database.itineraryItemDao().nextPositionSync(tripId, newDay);
                database.itineraryItemDao().updateDetails(
                        itemId, newDay, time, durationMin, booked, position);
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not update stop: " + e.getMessage()));
            }
        });
    }

    @Override
    public void deleteItineraryItem(String itemId, Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                database.itineraryItemDao().deleteById(itemId);
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not delete stop: " + e.getMessage()));
            }
        });
    }

    @Override
    public void applyItemOrder(List<String> orderedItemIds, Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                for (int i = 0; i < orderedItemIds.size(); i++) {
                    database.itineraryItemDao().updatePosition(orderedItemIds.get(i), i);
                }
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not reorder stops: " + e.getMessage()));
            }
        });
    }

    /** Gives the sample trip its details and sample flag in the store, once (keeps later edits). */
    private void seedStoreState() {
        PrototypeStore store = PrototypeStore.get();
        if (!store.hasMeta(SEED_TRIP_ID)) {
            store.setMeta(SEED_TRIP_ID, new TripMeta("Tokyo",
                    TripDates.label(SEED_START_DATE, SEED_DAYS), SEED_DAYS,
                    store.getDefaultHomeCurrency(), SEED_START_DATE));
            store.setSample(SEED_TRIP_ID, true);
        }
    }

    private void insertSeedStop(String name, int day, int position, Integer durationMin,
                                 boolean booked, String time) {
        database.itineraryItemDao().insert(new ItineraryItemEntity(
                UUID.randomUUID().toString(), SEED_TRIP_ID, null, name, day, time, durationMin,
                booked, null, null, position));
    }

    private <T> void deliver(Result.Callback<T> callback, Result<T> result) {
        executors.mainThread().execute(() -> callback.onResult(result));
    }
}
