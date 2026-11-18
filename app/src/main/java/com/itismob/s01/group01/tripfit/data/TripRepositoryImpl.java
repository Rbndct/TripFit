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
import com.itismob.s01.group01.tripfit.data.local.dao.ItineraryItemDao;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.ItemDetails;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.StopFormRules;
import com.itismob.s01.group01.tripfit.prototype.TripDates;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;
import com.itismob.s01.group01.tripfit.prototype.WeatherPlanning;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TripRepositoryImpl implements TripRepository {

    /** Fixed id so the Phase 0 seed trip is idempotent across app restarts. */
    public static final String SEED_TRIP_ID = "seed-trip-tokyo";
    private static final String SEED_START_DATE = "2026-12-01";
    private static final int SEED_DAYS = 2;
    private static final String TAG = "TripRepository";
    private static final String PIN_PREFIX = "pin-";
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
    public LiveData<ItineraryItemWithPlace> observeItemWithPlace(String itemId) {
        return database.itineraryItemDao().observeByIdWithPlace(itemId);
    }

    @Override
    public void addItineraryItem(String itemId, String tripId, String customName, int day,
                                 String time, Integer durationMin, boolean booked,
                                 Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                int position = database.itineraryItemDao().nextPositionSync(tripId, day);
                ItineraryItemEntity item = new ItineraryItemEntity(
                        itemId, tripId, null, customName, day, time,
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
    public void addItineraryItemFromPlace(String itemId, String tripId, PlaceDto place, int day,
                                          String time, Integer durationMin, boolean booked,
                                          Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                cachePlace(place);
                int position = database.itineraryItemDao().nextPositionSync(tripId, day);
                ItineraryItemEntity item = new ItineraryItemEntity(
                        itemId, tripId, place.getId(), null, day, time,
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
    public void addCatalogPlaces(String tripId, List<DemoCatalog.Place> places, int targetDay,
                                 boolean spread, Result.Callback<Void> callback) {
        List<DemoCatalog.Place> picks = new ArrayList<>(places);
        writeCatalog(callback, previous -> {
            int tripDays = validatedTripDays(tripId, targetDay);
            Set<String> names = catalogNames(tripId);
            for (int i = 0; i < picks.size(); i++) {
                DemoCatalog.Place place = picks.get(i);
                requireUnplanned(names, place);
                int day = CatalogSuggestions.dayForPick(targetDay, tripDays, i, spread);
                insertCatalogStop(UUID.randomUUID().toString(), tripId, place, day, null,
                        place.minutes, false, place.itemDetails(), previous);
            }
        });
    }

    @Override
    public void addCatalogPlace(String itemId, String tripId, DemoCatalog.Place place, int day,
                                String time, int durationMin, boolean booked, ItemDetails details,
                                Result.Callback<Void> callback) {
        writeCatalog(callback, previous -> {
            validatedTripDays(tripId, day);
            requireUnplanned(catalogNames(tripId), place);
            insertCatalogStop(itemId, tripId, place, day, time, durationMin, booked,
                    details, previous);
        });
    }

    @Override
    public void replaceWithCatalogPlace(String tripId, String itemId, DemoCatalog.Place place,
                                        Result.Callback<Void> callback) {
        writeCatalog(callback, previous -> {
            requireUnplanned(catalogNames(tripId), place);
            cacheCatalogPlace(place);
            if (database.itineraryItemDao().replaceFlexiblePlace(tripId, itemId,
                    place.id, place.minutes) != 1) {
                throw new IllegalStateException(CATALOG_STOP_UNAVAILABLE);
            }
            saveCatalogDetails(itemId, place.itemDetails(), previous);
        });
    }

    @Override
    public void swapWeatherStops(String tripId, String outdoorId, String indoorId,
                                 Result.Callback<Void> callback) {
        runVoid("", callback, () -> database.runInTransaction(() -> {
            ItineraryItemDao dao = database.itineraryItemDao();
            ItineraryItemWithPlace outdoor = null;
            ItineraryItemWithPlace indoor = null;
            for (ItineraryItemWithPlace stop : dao.listWithPlaceSync(tripId)) {
                if (stop.item.getId().equals(outdoorId)) {
                    outdoor = stop;
                } else if (stop.item.getId().equals(indoorId)) {
                    indoor = stop;
                }
            }
            if (outdoor == null || indoor == null
                    || !WeatherPlanning.canTradeDays(outdoor, indoor, PrototypeStore.get())) {
                throw new IllegalStateException(CATALOG_STOP_UNAVAILABLE);
            }
            dao.updateDayAndPosition(outdoorId, indoor.item.getDay(), indoor.item.getPosition());
            dao.updateDayAndPosition(indoorId, outdoor.item.getDay(), outdoor.item.getPosition());
        }));
    }

    private int validatedTripDays(String tripId, int day) {
        int days = PrototypeStore.get().meta(tripId).getDays();
        if (day < 1 || day > days) {
            throw new IllegalArgumentException("Day out of range");
        }
        return days;
    }

    private Set<String> catalogNames(String tripId) {
        return CatalogSuggestions.plannedNames(database.itineraryItemDao().listWithPlaceSync(tripId));
    }

    private static void requireUnplanned(Set<String> names, DemoCatalog.Place place) {
        if (!names.add(DemoCatalog.nameKey(place.name))) {
            throw new IllegalStateException(CATALOG_ALREADY_PLANNED);
        }
    }

    private void insertCatalogStop(String id, String tripId, DemoCatalog.Place place, int day,
                                   String time, int minutes, boolean booked, ItemDetails details,
                                   Map<String, ItemDetails> previous) {
        cacheCatalogPlace(place);
        ItineraryItemDao dao = database.itineraryItemDao();
        dao.insertNew(new ItineraryItemEntity(id, tripId, place.id, null, day, time, minutes,
                booked, null, null, dao.nextPositionSync(tripId, day)));
        saveCatalogDetails(id, details, previous);
    }

    private void cacheCatalogPlace(DemoCatalog.Place place) {
        cacheCity(OfflineData.toDto(place.catalogCity));
        cachePlace(OfflineData.toDto(place));
    }

    private void cachePlace(PlaceDto place) {
        database.placeDao().upsert(new PlaceEntity(
                place.getId(), place.getCityId(), place.getName(), place.getCategory(),
                place.getLat(), place.getLng(), place.getAddress(), place.getPriceTier(),
                place.getRating(), place.isHiddenGem(), place.getDescription(),
                place.getImageUrl(), null, null, null, null
        ));
    }

    private void cacheCity(CityDto city) {
        database.cityDao().upsert(new CityEntity(
                city.getId(), city.getName(), city.getCountry(), city.getLat(), city.getLng(),
                null, null, System.currentTimeMillis()));
    }

    private void writeCatalog(Result.Callback<Void> callback,
                              java.util.function.Consumer<Map<String, ItemDetails>> work) {
        executors.diskIO().execute(() -> {
            try {
                catalogTransaction(work);
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error(e.getMessage()));
            }
        });
    }

    private void catalogTransaction(java.util.function.Consumer<Map<String, ItemDetails>> work) {
        Map<String, ItemDetails> previous = new HashMap<>();
        try {
            database.runInTransaction(() -> work.accept(previous));
        } catch (RuntimeException e) {
            PrototypeStore store = PrototypeStore.get();
            previous.forEach((id, details) -> {
                if (details == null) {
                    store.removeItemDetails(id);
                } else {
                    store.setItemDetails(id, details);
                }
            });
            throw e;
        }
    }

    private static void saveCatalogDetails(String id, ItemDetails details,
                                           Map<String, ItemDetails> previous) {
        PrototypeStore store = PrototypeStore.get();
        previous.put(id, store.itemDetails(id));
        store.setItemDetails(id, details);
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
                catalogTransaction(previous -> {
                    if (database.tripDao().countSync() != 0) {
                        return;
                    }
                    database.tripDao().insert(new TripEntity(
                            SEED_TRIP_ID, null, "Tokyo Trip", null, null, null, null));
                    insertSeedStop("offline-senso-ji", 1, null, previous);
                    insertSeedStop("offline-nakamise", 1, null, previous);
                    insertSeedStop("offline-skytree", 1, "18:00", previous);
                    insertSeedStop("offline-meiji", 2, null, previous);
                    insertSeedStop("offline-shibuya", 2, null, previous);
                    insertSeedStop("offline-teamlab", 2, "15:30", previous);
                });
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
            List<CityDto> cities = new ArrayList<>();
            try {
                cities.addAll(remoteDataSource.fetchCitiesSync());
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "Supabase cities unavailable, using offline demo data: " + e.getMessage());
            }
            for (CityDto sample : OfflineData.cities()) {
                if (cities.stream().noneMatch(city -> sample.getName().equalsIgnoreCase(city.getName()))) {
                    cities.add(sample);
                }
            }
            try {
                cities.forEach(this::cacheCity);
                deliver(callback, Result.success(cities));
            } catch (RuntimeException e) {
                deliver(callback, Result.error(e.getMessage()));
            }
        });
    }

    @Override
    public void searchPlaces(String cityId, String query, Result.Callback<List<PlaceDto>> callback) {
        executors.diskIO().execute(() -> {
            String catalogCity = DemoCatalog.city(cityId) == null
                    ? database.cityDao().nameByIdSync(cityId) : cityId;
            List<PlaceDto> places = OfflineData.search(catalogCity, query);
            if (!query.isEmpty() && DemoCatalog.city(cityId) == null) {
                try {
                    List<PlaceDto> remote = remoteDataSource.searchPlacesSync(
                            cityId, query, PLACE_SEARCH_LIMIT);
                    for (PlaceDto place : remote) {
                        String name = DemoCatalog.nameKey(place.getName());
                        if (places.stream().noneMatch(sample ->
                                DemoCatalog.nameKey(sample.getName()).equals(name))) {
                            places.add(place);
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    Log.w(TAG, "Supabase search unavailable, using sample places: " + e.getMessage());
                }
            }
            deliver(callback, Result.success(places));
        });
    }

    @Override
    public void updateItineraryItem(String tripId, String itemId, int oldDay, int oldPosition,
                                    int newDay, String time, Integer durationMin, boolean booked,
                                    Integer bufferMin, String transportMode,
                                    Result.Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                int position = newDay == oldDay
                        ? oldPosition
                        : database.itineraryItemDao().nextPositionSync(tripId, newDay);
                database.itineraryItemDao().updateDetailsWithRouting(itemId, newDay, time,
                        durationMin, booked, position, bufferMin, transportMode);
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error("Could not update stop: " + e.getMessage()));
            }
        });
    }

    @Override
    public void updateLegSettings(String itemId, Integer bufferMin, String modeOrNull,
                                  Result.Callback<Void> callback) {
        runVoid("Could not save leg: ", callback, () -> database.itineraryItemDao()
                .updateLegSettings(itemId, bufferMin, modeOrNull));
    }

    @Override
    public void updateBuffer(String itemId, Integer bufferMin, Result.Callback<Void> callback) {
        runVoid("Could not save buffer: ", callback,
                () -> database.itineraryItemDao().updateBuffer(itemId, bufferMin));
    }

    @Override
    public void updateTransportMode(String itemId, String modeOrNull,
                                    Result.Callback<Void> callback) {
        runVoid("Could not save travel mode: ", callback,
                () -> database.itineraryItemDao().updateTransportMode(itemId, modeOrNull));
    }

    @Override
    public void adjustPin(String itemId, String cityId, String name, double lat, double lng,
                          Result.Callback<Void> callback) {
        if (!StopFormRules.isPinValid(lat, lng)) {
            deliver(callback, Result.error("Latitude must be -90 to 90 and longitude -180 to 180"));
            return;
        }
        runVoid("Could not save pin: ", callback, () ->
                database.placeDao().pinItem(itemId, PIN_PREFIX + itemId, cityId, name, lat, lng));
    }

    @Override
    public void updateItineraryItemWithPin(String tripId, String itemId, int oldDay,
                                           int oldPosition, int newDay, String time,
                                           Integer durationMin, boolean booked, Integer bufferMin,
                                           String transportMode, String pinName, double pinLat,
                                           double pinLng, Result.Callback<Void> callback) {
        if (!StopFormRules.isPinValid(pinLat, pinLng)) {
            deliver(callback, Result.error("Latitude must be -90 to 90 and longitude -180 to 180"));
            return;
        }
        runVoid("Could not update stop: ", callback, () -> database.runInTransaction(() -> {
            database.placeDao().pinItem(itemId, PIN_PREFIX + itemId, null, pinName, pinLat, pinLng);
            int position = newDay == oldDay
                    ? oldPosition
                    : database.itineraryItemDao().nextPositionSync(tripId, newDay);
            database.itineraryItemDao().updateDetailsWithRouting(itemId, newDay, time,
                    durationMin, booked, position, bufferMin, transportMode);
        }));
    }

    @Override
    public void applyMoves(List<TripSnapshots.ItemMove> moves, Result.Callback<Void> callback) {
        runVoid("Could not move stops: ", callback,
                () -> database.itineraryItemDao().applyMoves(moves));
    }

    @Override
    public void applyItemOrders(List<List<String>> orderedItemIdsPerDay,
                                Result.Callback<Void> callback) {
        runVoid("Could not reorder stops: ", callback,
                () -> database.itineraryItemDao().applyOrders(orderedItemIdsPerDay));
    }

    /** Runs a disk write off the main thread and reports success or a short error. */
    private void runVoid(String errorPrefix, Result.Callback<Void> callback, Runnable work) {
        executors.diskIO().execute(() -> {
            try {
                work.run();
                deliver(callback, Result.success(null));
            } catch (RuntimeException e) {
                deliver(callback, Result.error(errorPrefix + e.getMessage()));
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

    private void insertSeedStop(String placeId, int day, String time,
                                Map<String, ItemDetails> previous) {
        DemoCatalog.Place place = DemoCatalog.findById(placeId);
        insertCatalogStop(UUID.randomUUID().toString(), SEED_TRIP_ID, place, day, time,
                place.minutes, time != null, place.itemDetails(), previous);
    }

    private <T> void deliver(Result.Callback<T> callback, Result<T> result) {
        executors.mainThread().execute(() -> callback.onResult(result));
    }
}
