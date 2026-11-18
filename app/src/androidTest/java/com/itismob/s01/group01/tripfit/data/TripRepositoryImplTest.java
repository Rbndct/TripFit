package com.itismob.s01.group01.tripfit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.entity.CityEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.ItemDetails;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TripRepositoryImplTest {

    private TripFitDatabase database;
    private TripRepository repository;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), TripFitDatabase.class
        ).allowMainThreadQueries().build();
        com.itismob.s01.group01.tripfit.prototype.PrototypeStore.resetForTesting();
        repository = new TripRepositoryImpl(database, AppExecutors.getInstance(), new RemoteDataSource());
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void ensureSeedData_onEmptyDb_insertsTokyoTripWithSixStops() throws InterruptedException {
        Result<Void> result = awaitSeed();

        assertTrue(result.isSuccess());
        assertEquals(1, database.tripDao().countSync());
    }

    @Test
    public void ensureSeedData_calledTwice_doesNotDuplicateTrip() throws InterruptedException {
        awaitSeed();
        awaitSeed();

        assertEquals(1, database.tripDao().countSync());
    }

    @Test
    public void addItineraryItem_assignsIncrementingPositionWithinSameDay() throws InterruptedException {
        awaitSeed();
        List<ItineraryItemEntity> before = awaitItems("seed-trip-tokyo");
        int day1CountBefore = countForDay(before, 1);

        awaitAdd("seed-trip-tokyo", "Ueno Zoo", 1, null, false, null);

        List<ItineraryItemEntity> after = awaitItems("seed-trip-tokyo");
        ItineraryItemEntity added = after.stream()
                .filter(item -> "Ueno Zoo".equals(item.getCustomName()))
                .findFirst()
                .orElseThrow();

        assertEquals(day1CountBefore, added.getPosition());
    }

    @Test
    public void createTrip_insertsTripAndReturnsItsId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Result<String>[] holder = new Result[1];
        repository.createTrip("Manila Trip", result -> {
            holder[0] = result;
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);

        assertTrue(holder[0].isSuccess());
        assertEquals(1, database.tripDao().countSync());
    }

    @Test
    public void addItineraryItemFromPlace_cachesPlaceAndJoinsItsNameOnRead() throws InterruptedException {
        database.cityDao().upsert(new CityEntity(
                "city-1", "Tokyo", "JP", 35.68, 139.76, null, null, 0L));
        awaitSeed();
        PlaceDto place = new PlaceDto("place-1", "city-1", "Ueno Zoo", "zoo",
                35.71, 139.77, null, null, null, false, null, null);

        CountDownLatch latch = new CountDownLatch(1);
        repository.addItineraryItemFromPlace("seed-trip-tokyo", place, 1, null, null, false,
                result -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);

        List<ItineraryItemWithPlace> items = awaitItemsWithPlace("seed-trip-tokyo");
        ItineraryItemWithPlace added = items.stream()
                .filter(i -> "place-1".equals(i.item.getPlaceId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Ueno Zoo", added.placeName);
    }

    @Test
    public void renameTrip_updatesNameAndKeepsStops() throws InterruptedException {
        awaitSeed();
        int stopsBefore = awaitItems("seed-trip-tokyo").size();

        CountDownLatch latch = new CountDownLatch(1);
        repository.renameTrip("seed-trip-tokyo", "Renamed", result -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);

        assertEquals(stopsBefore, awaitItems("seed-trip-tokyo").size());
        assertEquals(1, database.tripDao().countSync());
        assertEquals("Renamed", awaitTrips().get(0).getName());
    }

    @Test
    public void observeAll_listsNewestTripFirst() throws InterruptedException {
        database.tripDao().insert(new com.itismob.s01.group01.tripfit.data.local.entity.TripEntity(
                "trip-a", null, "A", null, null, null, null));
        database.tripDao().insert(new com.itismob.s01.group01.tripfit.data.local.entity.TripEntity(
                "trip-b", null, "B", null, null, null, null));

        List<com.itismob.s01.group01.tripfit.data.local.entity.TripEntity> trips = awaitTrips();

        assertEquals("trip-b", trips.get(0).getId());
        assertEquals("trip-a", trips.get(1).getId());
    }

    @Test
    public void maxStopDay_reportsLastDayWithAStop() throws InterruptedException {
        awaitSeed();

        CountDownLatch latch = new CountDownLatch(1);
        Integer[] holder = new Integer[1];
        repository.maxStopDay("seed-trip-tokyo", result -> {
            holder[0] = result.getData();
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);

        assertEquals(Integer.valueOf(2), holder[0]);
    }

    @Test
    public void ensureSeedData_marksSeedTripAsSampleWithDetailsInStore() throws InterruptedException {
        awaitSeed();

        com.itismob.s01.group01.tripfit.prototype.PrototypeStore store =
                com.itismob.s01.group01.tripfit.prototype.PrototypeStore.get();
        assertTrue(store.isSample("seed-trip-tokyo"));
        assertTrue(store.hasMeta("seed-trip-tokyo"));
        assertEquals("Tokyo", store.meta("seed-trip-tokyo").getCity());
        assertEquals(2, store.meta("seed-trip-tokyo").getDays());
    }

    // ---- routing fields, pins and batch updates (compile-only here; run on a device) ----

    private ItineraryItemEntity firstItem(String name) throws InterruptedException {
        return awaitItemsWithPlace("seed-trip-tokyo").stream()
                .filter(row -> name.equals(row.item.displayName(row.placeName)))
                .map(row -> row.item).findFirst().orElseThrow();
    }

    private Result<Void> awaitVoid(java.util.function.Consumer<Result.Callback<Void>> call)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Result<Void>[] holder = new Result[1];
        call.accept(result -> {
            holder[0] = result;
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
        return holder[0];
    }

    @Test
    public void updateBufferAndMode_writeOnlyTheirColumns() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity original = firstItem("Senso-ji Temple");
        String id = original.getId();

        awaitVoid(cb -> repository.updateBuffer(id, 0, cb));
        awaitVoid(cb -> repository.updateTransportMode(id, "CYCLE", cb));

        ItineraryItemEntity item = firstItem("Senso-ji Temple");
        assertEquals(Integer.valueOf(0), item.getBufferMin());
        assertEquals("CYCLE", item.getTransportMode());
        assertEquals(original.getDurationMin(), item.getDurationMin());

        awaitVoid(cb -> repository.updateTransportMode(id, null, cb));
        assertEquals(null, firstItem("Senso-ji Temple").getTransportMode());
    }

    @Test
    public void updateItineraryItem_withRouting_savesEverythingInOneUpdate() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity before = firstItem("Meiji Shrine");

        awaitVoid(cb -> repository.updateItineraryItem("seed-trip-tokyo", before.getId(),
                before.getDay(), before.getPosition(), before.getDay(), null, 50, false, 10,
                "WALK", cb));

        ItineraryItemEntity after = firstItem("Meiji Shrine");
        assertEquals(Integer.valueOf(50), after.getDurationMin());
        assertEquals(Integer.valueOf(10), after.getBufferMin());
        assertEquals("WALK", after.getTransportMode());
        assertEquals(before.getPosition(), after.getPosition());
    }

    @Test
    public void adjustPin_createsAPerItemPlaceAndLeavesOtherItemsAlone() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity pinned = firstItem("Senso-ji Temple");
        ItineraryItemEntity other = firstItem("Meiji Shrine");
        ItineraryItemWithPlace originalOther = awaitSingle(other.getId());

        Result<Void> result = awaitVoid(cb -> repository.adjustPin(pinned.getId(), null,
                "Senso-ji Temple", 35.7148, 139.7967, cb));

        assertTrue(result.isSuccess());
        List<ItineraryItemWithPlace> rows = awaitItemsWithPlace("seed-trip-tokyo");
        ItineraryItemWithPlace pinnedRow = rows.stream()
                .filter(r -> r.item.getId().equals(pinned.getId())).findFirst().orElseThrow();
        ItineraryItemWithPlace otherRow = rows.stream()
                .filter(r -> r.item.getId().equals(other.getId())).findFirst().orElseThrow();
        assertEquals("pin-" + pinned.getId(), pinnedRow.item.getPlaceId());
        assertEquals(35.7148, pinnedRow.placeLat, 0.00001);
        assertEquals(139.7967, pinnedRow.placeLng, 0.00001);
        assertEquals(originalOther.placeLat, otherRow.placeLat);
        assertEquals(originalOther.placeLng, otherRow.placeLng);
        assertEquals(1, database.placeDao().countByIdSync("pin-" + pinned.getId()));
    }

    @Test
    public void adjustPin_onItemWithCatalogPlace_keepsItsDetailsAndMovesCoordinates() throws InterruptedException {
        database.cityDao().upsert(new CityEntity(
                "city-1", "Tokyo", "JP", 35.68, 139.76, null, null, 0L));
        awaitSeed();
        database.placeDao().upsert(new com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity(
                "place-9", "city-1", "Ueno Zoo", "zoo", 35.71, 139.77, "Ueno Park", 2, 4.5,
                false, null, "http://img", null, null, null, null));
        String id = firstItem("Senso-ji Temple").getId();
        database.itineraryItemDao().updatePlace(id, "place-9");

        Result<Void> result = awaitVoid(cb -> repository.adjustPin(id, null, "Ueno Zoo", 10, 20, cb));

        assertTrue(result.isSuccess());
        com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity pin =
                database.placeDao().findByIdSync("pin-" + id);
        assertEquals("zoo", pin.getCategory());
        assertEquals("Ueno Park", pin.getAddress());
        assertEquals(Integer.valueOf(2), pin.getPriceTier());
        assertEquals(Double.valueOf(4.5), pin.getRating());
        assertEquals("http://img", pin.getImageUrl());
        assertEquals("city-1", pin.getCityId());
        assertEquals(10.0, pin.getLat(), 0.00001);
        assertEquals(20.0, pin.getLng(), 0.00001);
        // The shared catalog place is untouched.
        assertEquals(35.71, database.placeDao().findByIdSync("place-9").getLat(), 0.00001);
    }

    @Test
    public void adjustPin_repeated_isIdempotentAndKeepsDetails() throws InterruptedException {
        database.cityDao().upsert(new CityEntity(
                "city-1", "Tokyo", "JP", 35.68, 139.76, null, null, 0L));
        awaitSeed();
        database.placeDao().upsert(new com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity(
                "place-9", "city-1", "Ueno Zoo", "zoo", 35.71, 139.77, "Ueno Park", 2, 4.5,
                false, null, null, null, null, null, null));
        String id = firstItem("Senso-ji Temple").getId();
        database.itineraryItemDao().updatePlace(id, "place-9");

        awaitVoid(cb -> repository.adjustPin(id, null, "Ueno Zoo", 10, 20, cb));
        awaitVoid(cb -> repository.adjustPin(id, null, "Ueno Zoo", 11, 21, cb));
        Result<Void> third = awaitVoid(cb -> repository.adjustPin(id, null, "Ueno Zoo", 11, 21, cb));

        assertTrue(third.isSuccess());
        assertEquals(1, database.placeDao().countByIdSync("pin-" + id));
        com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity pin =
                database.placeDao().findByIdSync("pin-" + id);
        assertEquals(11.0, pin.getLat(), 0.00001);
        assertEquals("Ueno Park", pin.getAddress());
        assertEquals("city-1", pin.getCityId());
        assertEquals("pin-" + id, awaitSingle(id).item.getPlaceId());
    }

    @Test
    public void updateItineraryItemWithPin_writesStopAndPinTogether() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity before = firstItem("Meiji Shrine");

        Result<Void> result = awaitVoid(cb -> repository.updateItineraryItemWithPin(
                "seed-trip-tokyo", before.getId(), before.getDay(), before.getPosition(),
                before.getDay(), null, 50, false, null, "WALK", "Meiji Shrine", 35.67, 139.69, cb));

        assertTrue(result.isSuccess());
        ItineraryItemEntity after = firstItem("Meiji Shrine");
        assertEquals(Integer.valueOf(50), after.getDurationMin());
        assertEquals(null, after.getBufferMin());
        assertEquals("pin-" + before.getId(), after.getPlaceId());
    }

    @Test
    public void updateItineraryItemWithPin_invalidPinChangesNothing() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity before = firstItem("Meiji Shrine");

        Result<Void> result = awaitVoid(cb -> repository.updateItineraryItemWithPin(
                "seed-trip-tokyo", before.getId(), before.getDay(), before.getPosition(),
                before.getDay(), null, 50, false, null, null, "x", 95, 0, cb));

        assertTrue(!result.isSuccess());
        assertEquals(before.getDurationMin(), firstItem("Meiji Shrine").getDurationMin());
        assertEquals(0, database.placeDao().countByIdSync("pin-" + before.getId()));
    }

    @Test
    public void adjustPin_rejectsOutOfRangeCoordinatesWithoutWriting() throws InterruptedException {
        awaitSeed();
        String id = firstItem("Senso-ji Temple").getId();

        Result<Void> badLat = awaitVoid(cb -> repository.adjustPin(id, null, "x", 91, 0,
                cb));
        Result<Void> badLng = awaitVoid(cb -> repository.adjustPin(id, null, "x", 0, -180.5,
                cb));

        assertTrue(!badLat.isSuccess());
        assertTrue(!badLng.isSuccess());
        assertEquals(0, database.placeDao().countByIdSync("pin-" + id));
    }

    @Test
    public void observeItemWithPlace_emitsTheItemWithItsPinAndNullAfterDelete() throws InterruptedException {
        awaitSeed();
        String id = firstItem("Senso-ji Temple").getId();
        awaitVoid(cb -> repository.adjustPin(id, null, "Pin", 10, 20, cb));

        ItineraryItemWithPlace row = awaitSingle(id);
        assertEquals("Pin", row.placeName);
        assertEquals(10.0, row.placeLat, 0.0001);

        awaitVoid(cb -> repository.deleteItineraryItem(id, cb));
        assertEquals(null, awaitSingle(id));
    }

    @Test
    public void applyMoves_updatesDayAndPositionTogether() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity a = firstItem("Senso-ji Temple");
        ItineraryItemEntity b = firstItem("Meiji Shrine");

        Result<Void> result = awaitVoid(cb -> repository.applyMoves(java.util.Arrays.asList(
                new com.itismob.s01.group01.tripfit.prototype.TripSnapshots.ItemMove(a.getId(), 2, 7),
                new com.itismob.s01.group01.tripfit.prototype.TripSnapshots.ItemMove(b.getId(), 1, 3)),
                cb));

        assertTrue(result.isSuccess());
        assertEquals(2, firstItem("Senso-ji Temple").getDay());
        assertEquals(7, firstItem("Senso-ji Temple").getPosition());
        assertEquals(1, firstItem("Meiji Shrine").getDay());
        assertEquals(3, firstItem("Meiji Shrine").getPosition());
    }

    @Test
    public void applyItemOrders_rewritesSeveralDaysInOneCall() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity a = firstItem("Senso-ji Temple");
        ItineraryItemEntity b = firstItem("Nakamise Shopping Street");
        ItineraryItemEntity c = firstItem("Meiji Shrine");
        ItineraryItemEntity d = firstItem("Shibuya Crossing");

        Result<Void> result = awaitVoid(cb -> repository.applyItemOrders(java.util.Arrays.asList(
                java.util.Arrays.asList(b.getId(), a.getId()),
                java.util.Arrays.asList(d.getId(), c.getId())), cb));

        assertTrue(result.isSuccess());
        assertEquals(0, firstItem("Nakamise Shopping Street").getPosition());
        assertEquals(1, firstItem("Senso-ji Temple").getPosition());
        assertEquals(0, firstItem("Shibuya Crossing").getPosition());
        assertEquals(1, firstItem("Meiji Shrine").getPosition());
    }

    @Test
    public void catalogAdd_savesMetadataOnTargetDayAndRejectsDuplicateAcrossDays()
            throws InterruptedException {
        awaitSeed();
        DemoCatalog.Place place = DemoCatalog.findById("offline-nezu-museum");
        Result<Void> added = awaitVoid(cb -> repository.addCatalogPlaces("seed-trip-tokyo",
                java.util.Collections.singletonList(place), 2, false, cb));

        assertTrue(added.isSuccess());
        ItineraryItemEntity item = firstItem(place.name);
        ItineraryItemWithPlace row = awaitSingle(item.getId());
        assertEquals(2, item.getDay());
        assertEquals(Integer.valueOf(place.minutes), item.getDurationMin());
        assertEquals(place.lat, row.placeLat, 0.00001);
        assertEquals(place.lng, row.placeLng, 0.00001);
        ItemDetails details = PrototypeStore.get().itemDetails(item.getId());
        assertEquals(place.price, details.getCost(), 0.0);
        assertEquals("JPY", details.getCostCurrency());
        assertEquals(place.category, details.getCategory());
        assertFalse(details.isOutdoor());

        Result<Void> duplicate = awaitVoid(cb -> repository.addCatalogPlaces("seed-trip-tokyo",
                java.util.Collections.singletonList(place), 1, false, cb));
        assertEquals(TripRepository.CATALOG_ALREADY_PLANNED, duplicate.getErrorMessage());
        assertEquals(7, awaitItems("seed-trip-tokyo").size());
    }

    @Test
    public void catalogSwap_keepsLegAndRejectsBookedStops() throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity original = firstItem("Senso-ji Temple");
        awaitVoid(cb -> repository.updateLegSettings(original.getId(), 10, "WALK", cb));
        DemoCatalog.Place place = DemoCatalog.findById("offline-nezu-museum");
        assertTrue(awaitVoid(cb -> repository.replaceWithCatalogPlace("seed-trip-tokyo",
                original.getId(), place, cb)).isSuccess());
        ItineraryItemWithPlace row = awaitSingle(original.getId());
        assertEquals(place.name, row.placeName);
        assertEquals(place.lat, row.placeLat, 0.00001);
        assertEquals(original.getDay(), row.item.getDay());
        assertEquals(original.getPosition(), row.item.getPosition());
        assertEquals(Integer.valueOf(10), row.item.getBufferMin());
        assertEquals("WALK", row.item.getTransportMode());
        assertEquals(place.price, PrototypeStore.get().itemDetails(row.item.getId()).getCost(), 0.0);

        ItineraryItemEntity skytree = firstItem("Tokyo Skytree");
        Result<Void> blocked = awaitVoid(cb -> repository.replaceWithCatalogPlace("seed-trip-tokyo",
                skytree.getId(), DemoCatalog.findById("offline-nezu"), cb));
        assertEquals(TripRepository.CATALOG_STOP_UNAVAILABLE, blocked.getErrorMessage());
        assertEquals("18:00", firstItem("Tokyo Skytree").getTime());
    }

    @Test
    public void weatherSwap_exchangesSlotsAndKeepsStopDetails() throws InterruptedException {
        awaitSeed();
        String trip = TripRepositoryImpl.SEED_TRIP_ID;
        ItineraryItemEntity outdoor = firstItem("Meiji Shrine");
        DemoCatalog.Place museum = DemoCatalog.findById("offline-nezu-museum");
        assertTrue(awaitVoid(cb -> repository.addCatalogPlaces(trip,
                java.util.Collections.singletonList(museum), 1, false, cb)).isSuccess());
        ItineraryItemEntity indoor = firstItem(museum.name);
        awaitVoid(cb -> repository.updateLegSettings(outdoor.getId(), 12, "WALK", cb));

        assertTrue(awaitVoid(cb -> repository.swapWeatherStops(trip,
                outdoor.getId(), indoor.getId(), cb)).isSuccess());

        ItineraryItemEntity movedOutdoor = awaitSingle(outdoor.getId()).item;
        ItineraryItemEntity movedIndoor = awaitSingle(indoor.getId()).item;
        assertEquals(indoor.getDay(), movedOutdoor.getDay());
        assertEquals(indoor.getPosition(), movedOutdoor.getPosition());
        assertEquals(outdoor.getDay(), movedIndoor.getDay());
        assertEquals(outdoor.getPosition(), movedIndoor.getPosition());
        assertEquals(outdoor.getDurationMin(), movedOutdoor.getDurationMin());
        assertEquals(Integer.valueOf(12), movedOutdoor.getBufferMin());
        assertEquals("WALK", movedOutdoor.getTransportMode());
        assertEquals(indoor.getPlaceId(), movedIndoor.getPlaceId());
    }

    @Test
    public void weatherSwap_rejectsBookedAlternativeWithoutMovingEitherStop()
            throws InterruptedException {
        awaitSeed();
        ItineraryItemEntity outdoor = firstItem("Meiji Shrine");
        ItineraryItemEntity booked = firstItem("Tokyo Skytree");

        Result<Void> result = awaitVoid(cb -> repository.swapWeatherStops(
                TripRepositoryImpl.SEED_TRIP_ID, outdoor.getId(), booked.getId(), cb));

        assertEquals(TripRepository.CATALOG_STOP_UNAVAILABLE, result.getErrorMessage());
        assertEquals(outdoor.getDay(), awaitSingle(outdoor.getId()).item.getDay());
        assertEquals(booked.getDay(), awaitSingle(booked.getId()).item.getDay());
        assertEquals("18:00", awaitSingle(booked.getId()).item.getTime());
    }

    private ItineraryItemWithPlace awaitSingle(String id) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ItineraryItemWithPlace[] holder = new ItineraryItemWithPlace[1];
        androidx.lifecycle.Observer<ItineraryItemWithPlace> observer = row -> {
            holder[0] = row;
            latch.countDown();
        };
        androidx.lifecycle.LiveData<ItineraryItemWithPlace> liveData =
                repository.observeItemWithPlace(id);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.observeForever(observer));
        latch.await(5, TimeUnit.SECONDS);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.removeObserver(observer));
        return holder[0];
    }

    private List<com.itismob.s01.group01.tripfit.data.local.entity.TripEntity> awaitTrips()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<com.itismob.s01.group01.tripfit.data.local.entity.TripEntity>[] holder = new List[1];
        androidx.lifecycle.Observer<List<com.itismob.s01.group01.tripfit.data.local.entity.TripEntity>>
                observer = trips -> {
                    holder[0] = trips;
                    latch.countDown();
                };
        androidx.lifecycle.LiveData<List<com.itismob.s01.group01.tripfit.data.local.entity.TripEntity>>
                liveData = repository.observeTrips();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.observeForever(observer));
        latch.await(5, TimeUnit.SECONDS);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.removeObserver(observer));
        return holder[0];
    }

    private List<ItineraryItemWithPlace> awaitItemsWithPlace(String tripId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<ItineraryItemWithPlace>[] holder = new List[1];
        androidx.lifecycle.Observer<List<ItineraryItemWithPlace>> observer = items -> {
            holder[0] = items;
            latch.countDown();
        };
        androidx.lifecycle.LiveData<List<ItineraryItemWithPlace>> liveData =
                repository.observeItineraryItemsWithPlaceNames(tripId);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.observeForever(observer));
        latch.await(5, TimeUnit.SECONDS);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.removeObserver(observer));
        return holder[0];
    }

    private int countForDay(List<ItineraryItemEntity> items, int day) {
        return (int) items.stream().filter(i -> i.getDay() == day).count();
    }

    private Result<Void> awaitSeed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Result<Void>[] holder = new Result[1];
        repository.ensureSeedData(result -> {
            holder[0] = result;
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
        return holder[0];
    }

    private void awaitAdd(String tripId, String name, int day, Integer durationMin,
                           boolean booked, String time) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        repository.addItineraryItem(tripId, name, day, time, durationMin, booked,
                result -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);
    }

    private List<ItineraryItemEntity> awaitItems(String tripId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<ItineraryItemEntity>[] holder = new List[1];
        androidx.lifecycle.Observer<List<ItineraryItemEntity>> observer = items -> {
            holder[0] = items;
            latch.countDown();
        };
        androidx.lifecycle.LiveData<List<ItineraryItemEntity>> liveData =
                repository.observeItineraryItems(tripId);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.observeForever(observer));
        latch.await(5, TimeUnit.SECONDS);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> liveData.removeObserver(observer));
        return holder[0];
    }
}
