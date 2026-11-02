package com.itismob.s01.group01.tripfit.data;

import androidx.lifecycle.LiveData;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.entity.CityEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class TripRepositoryImpl implements TripRepository {

    /** Fixed id so the Phase 0 seed trip is idempotent across app restarts. */
    private static final String SEED_TRIP_ID = "seed-trip-tokyo";
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
                database.tripDao().insert(new TripEntity(tripId, null, name, null, null, "USD", null));
                deliver(callback, Result.success(tripId));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not create trip: " + e.getMessage()));
            }
        });
    }

    @Override
    public void ensureSeedData(Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                if (database.tripDao().countSync() == 0) {
                    database.tripDao().insert(new TripEntity(
                            SEED_TRIP_ID, null, "Tokyo Trip", null, null, "USD", null));
                    insertSeedStop("Senso-ji Temple", 1, 0, 45, false, null);
                    insertSeedStop("Nakamise Shopping Street", 1, 1, 30, false, null);
                    insertSeedStop("Tokyo Skytree", 1, 2, null, true, "18:00");
                    insertSeedStop("Meiji Shrine", 2, 0, 40, false, null);
                    insertSeedStop("Shibuya Crossing", 2, 1, 20, false, null);
                    insertSeedStop("teamLab Planets", 2, 2, null, true, "15:30");
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
            try {
                List<CityDto> cities = remoteDataSource.fetchCitiesSync();
                // Cache locally so places selected from search can reference a city that
                // already satisfies PlaceEntity's foreign key.
                for (CityDto city : cities) {
                    database.cityDao().upsert(new CityEntity(
                            city.getId(), city.getName(), city.getCountry(), city.getLat(),
                            city.getLng(), null, null, System.currentTimeMillis()
                    ));
                }
                deliver(callback, Result.success(cities));
            } catch (IOException | RuntimeException e) {
                deliver(callback, Result.error("Could not load cities: " + e.getMessage()));
            }
        });
    }

    @Override
    public void searchPlaces(String cityId, String query, Result.Callback<List<PlaceDto>> callback) {
        executors.diskIO().execute(() -> {
            try {
                List<PlaceDto> places = remoteDataSource.searchPlacesSync(cityId, query, PLACE_SEARCH_LIMIT);
                deliver(callback, Result.success(places));
            } catch (IOException | RuntimeException e) {
                deliver(callback, Result.error("Search failed: " + e.getMessage()));
            }
        });
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
