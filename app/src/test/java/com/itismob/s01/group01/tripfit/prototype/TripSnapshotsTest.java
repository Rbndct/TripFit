package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripSnapshotsTest {

    private PrototypeStore store;
    private final TripMeta meta = new TripMeta("Tokyo", "", 2, "PHP");

    @Before
    public void setUp() {
        store = PrototypeStore.forStorage(new InMemoryKeyValueStorage());
    }

    private static ItineraryItemWithPlace row(String id, int day, int position, Integer duration,
                                              boolean booked, String time, String mode,
                                              Integer buffer, Double lat, Double lng) {
        ItineraryItemWithPlace row = new ItineraryItemWithPlace();
        row.item = new ItineraryItemEntity(id, "t", null, id, day, time, duration, booked, mode,
                buffer, position);
        row.placeLat = lat;
        row.placeLng = lng;
        return row;
    }

    private static ItineraryItemWithPlace plain(String id, int day, int position) {
        return row(id, day, position, 30, false, null, null, null, null, null);
    }

    private DraftTrip snapshot(ItineraryItemWithPlace... rows) {
        return TripSnapshots.from("t", meta, Arrays.asList(rows), store);
    }

    @Test
    public void missingDuration_usesDefaultStay() {
        DraftTrip trip = snapshot(row("a", 1, 0, null, false, null, null, null, null, null));
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, 1, false);
        assertEquals(ScheduleMath.DEFAULT_STAY_MIN, slots.get(0).end - slots.get(0).start);
    }

    @Test
    public void buffer_nullFallsBackToProfileAndExplicitZeroIsKept() {
        store.setBufferMin(40);
        DraftTrip trip = snapshot(
                row("a", 1, 0, 30, false, null, null, null, null, null),
                row("b", 1, 1, 30, false, null, null, 0, null, null),
                row("c", 1, 2, 30, false, null, null, 25, null, null));
        assertEquals(40, trip.stops.get(0).buffer);
        assertEquals(0, trip.stops.get(1).buffer);
        assertEquals(25, trip.stops.get(2).buffer);
    }

    @Test
    public void mode_mapsStoredNamesAndTreatsNullOrInvalidAsSuggested() {
        DraftTrip trip = snapshot(
                row("a", 1, 0, 30, false, null, "WALK", null, null, null),
                row("b", 1, 1, 30, false, null, "CYCLE", null, null, null),
                row("c", 1, 2, 30, false, null, "DRIVE", null, null, null),
                row("d", 1, 3, 30, false, null, "TRANSIT", null, null, null),
                row("e", 1, 4, 30, false, null, null, null, null, null),
                row("f", 1, 5, 30, false, null, "TELEPORT", null, null, null));
        String[] expected = {"Walking", "Cycling", "Driving", "Transit", "Suggested", "Suggested"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], trip.stops.get(i).mode);
        }
    }

    @Test
    public void pinned_requiresPlaceCoordinatesAndCarriesThem() {
        DraftTrip trip = snapshot(
                row("a", 1, 0, 30, false, null, null, null, 35.7, 139.8),
                plain("b", 1, 1));
        assertTrue(trip.stops.get(0).hasLocation);
        assertEquals(35.7, trip.stops.get(0).lat, 0.0001);
        assertEquals(139.8, trip.stops.get(0).lng, 0.0001);
        assertFalse(trip.stops.get(1).hasLocation);
    }

    @Test
    public void bookedStop_usesItsTimeElseNineAm() {
        DraftTrip trip = snapshot(
                row("a", 1, 0, 30, true, "18:00", null, null, null, null),
                row("b", 1, 1, 30, true, null, null, null, null, null),
                row("c", 1, 2, 30, true, "bad", null, null, null, null));
        assertTrue(trip.stops.get(0).booked);
        assertEquals(1080, trip.stops.get(0).fixedMinute);
        assertEquals(540, trip.stops.get(1).fixedMinute);
        assertEquals(540, trip.stops.get(2).fixedMinute);
    }

    @Test
    public void details_costCategoryAndOutdoorComeFromTheStore() {
        store.setItemDetails("a", new ItemDetails(1500, "JPY", "Food", false));
        DraftTrip trip = snapshot(plain("a", 1, 0), plain("b", 1, 1));
        assertEquals(1500, trip.stops.get(0).cost, 0.001);
        assertEquals("JPY", trip.stops.get(0).currency);
        assertEquals("Food", trip.stops.get(0).category);
        assertFalse(trip.stops.get(0).outdoor);
        assertTrue(trip.stops.get(1).outdoor);
    }

    @Test
    public void days_isMaxOfTripLengthAndLastStopDay() {
        assertEquals(2, snapshot(plain("a", 1, 0)).days);
        assertEquals(5, snapshot(plain("a", 5, 0)).days);
        assertEquals(2, snapshot().days);
    }

    @Test
    public void clampDay_keepsSelectionInsideTheTrip() {
        assertEquals(1, TripSnapshots.clampDay(0, 3));
        assertEquals(1, TripSnapshots.clampDay(-4, 3));
        assertEquals(2, TripSnapshots.clampDay(2, 3));
        assertEquals(3, TripSnapshots.clampDay(9, 3));
        assertEquals(1, TripSnapshots.clampDay(5, 0));
    }

    @Test
    public void schedule_includesTravelAndBufferAndSkipsEmptyDays() {
        store.setBufferMin(15);
        Map<Integer, List<RoutePreview.Slot>> byDay = TripSnapshots.schedule("t", meta,
                Arrays.asList(plain("a", 1, 0), plain("b", 1, 1), plain("c", 3, 0)), store);
        assertEquals(Arrays.asList(1, 3), new ArrayList<>(byDay.keySet()));
        assertEquals(540, byDay.get(1).get(0).start);
        assertEquals(570 + ScheduleMath.PLACEHOLDER_TRAVEL_MIN + 15, byDay.get(1).get(1).start);
    }

    @Test
    public void moves_renumberEachDayKeepingOldOrder() {
        List<ItineraryItemWithPlace> rows = Arrays.asList(
                plain("a", 1, 0), plain("b", 1, 1), plain("c", 1, 2), plain("d", 2, 0));
        Map<String, Integer> balance = new HashMap<>();
        balance.put("b", 2);
        List<TripSnapshots.ItemMove> moves = TripSnapshots.moves(balance, rows);
        Map<String, String> byId = new HashMap<>();
        for (TripSnapshots.ItemMove m : moves) {
            byId.put(m.id, m.day + ":" + m.position);
        }
        assertEquals("1:0", byId.get("a"));
        assertEquals("1:1", byId.get("c"));
        assertEquals("2:0", byId.get("b"));
        assertEquals("2:1", byId.get("d"));
    }

    @Test
    public void moves_emptyBalanceKeepsDaysAndCompactsPositions() {
        List<ItineraryItemWithPlace> rows = Arrays.asList(plain("a", 1, 4), plain("b", 1, 9));
        List<TripSnapshots.ItemMove> moves = TripSnapshots.moves(Collections.<String, Integer>emptyMap(), rows);
        assertEquals(0, moves.get(0).position);
        assertEquals(1, moves.get(1).position);
        assertEquals(1, moves.get(1).day);
    }

    private static Map<String, String> placement(List<TripSnapshots.ItemMove> moves) {
        Map<String, String> byId = new HashMap<>();
        for (TripSnapshots.ItemMove m : moves) {
            byId.put(m.id, m.day + ":" + m.position);
        }
        return byId;
    }

    @Test
    public void moves_movedInStopsOrderBySourceDayThenPosition() {
        List<ItineraryItemWithPlace> rows = Arrays.asList(
                plain("a", 1, 0), plain("b", 1, 1),
                plain("c", 2, 0), plain("d", 2, 1),
                plain("e", 3, 0), plain("f", 3, 1));
        Map<String, Integer> balance = new HashMap<>();
        balance.put("b", 2); // from an earlier day: lands before day 2's own stops
        balance.put("e", 2); // from a later day: lands after them
        balance.put("f", 2);
        Map<String, String> byId = placement(TripSnapshots.moves(balance, rows));
        assertEquals("1:0", byId.get("a"));
        assertEquals("2:0", byId.get("b"));
        assertEquals("2:1", byId.get("c"));
        assertEquals("2:2", byId.get("d"));
        assertEquals("2:3", byId.get("e"));
        assertEquals("2:4", byId.get("f"));
        assertEquals(6, byId.size());
    }

    @Test
    public void moves_resultIsGapFreeAndStableRegardlessOfInputOrder() {
        List<ItineraryItemWithPlace> rows = new ArrayList<>(Arrays.asList(
                plain("a", 1, 3), plain("b", 1, 8), plain("c", 2, 5), plain("d", 3, 2)));
        Map<String, Integer> balance = new HashMap<>();
        balance.put("a", 3);
        balance.put("d", 1);
        Map<String, String> first = placement(TripSnapshots.moves(balance, rows));
        Collections.reverse(rows);
        Map<String, String> second = placement(TripSnapshots.moves(balance, rows));
        assertEquals(first, second);
        assertEquals("1:0", first.get("b"));
        assertEquals("1:1", first.get("d"));
        assertEquals("2:0", first.get("c"));
        assertEquals("3:0", first.get("a"));
    }
    @Test
    public void exportSnapshotUsesSavedDateAddressAndBudget() {
        PrototypeStore store = PrototypeStore.forStorage(new InMemoryKeyValueStorage());
        store.setMeta("trip", new TripMeta("Manila", "", 2, "PHP", "2027-03-02"));
        store.setBudget("trip", new BudgetSettings("PHP", "PHP", 900, true,
                java.util.Collections.singletonMap(Category.FOOD, 400.0)));
        store.addExpense("trip", "Lunch", Category.FOOD, 200, "PHP");
        ItineraryItemWithPlace row = new ItineraryItemWithPlace();
        row.item = new ItineraryItemEntity("stop", "trip", "place", null, 1,
                null, 45, false, null, null, 0);
        row.placeName = "Cafe";
        row.placeAddress = "1 Main St, Manila";

        DraftTrip trip = TripSnapshots.forExport("trip", "Manila Break",
                java.util.Collections.singletonList(row), store);

        assertEquals("2027-03-02", trip.startDate);
        assertEquals("Manila Break", trip.name);
        assertEquals("1 Main St, Manila", trip.stops.get(0).address);
        assertEquals(1800, trip.totalBudget(), 0.0);
        assertEquals(800, trip.categoryBudget("Food"), 0.0);
        assertEquals(200, trip.expenses.get(0).amount, 0.0);
        assertTrue(TripExport.summary(trip).contains("No stops planned yet."));
    }
}
