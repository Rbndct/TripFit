package com.itismob.s01.group01.tripfit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;
import com.itismob.s01.group01.tripfit.data.local.entity.CityEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

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
