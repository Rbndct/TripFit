package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ports the RouteMath intents to RoutePreview semantics, plus the merged-engine behaviours. */
public class RoutePreviewMergeTest {

    private static DraftTrip.Stop stop(String id, int day, int duration, int buffer) {
        DraftTrip.Stop s = new DraftTrip.Stop();
        s.id = id;
        s.name = id;
        s.day = day;
        s.duration = duration;
        s.buffer = buffer;
        return s;
    }

    private static DraftTrip.Stop pinned(String id, double lng) {
        DraftTrip.Stop s = stop(id, 1, 30, 0);
        s.hasLocation = true;
        s.lat = 0;
        s.lng = lng;
        s.mode = "Driving";
        return s;
    }

    private static DraftTrip.Stop booked(String id, int day, int minute, int duration) {
        DraftTrip.Stop s = stop(id, day, duration, 0);
        s.booked = true;
        s.fixedMinute = minute;
        return s;
    }

    private static DraftTrip lineTrip() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(pinned("A", 0));
        trip.stops.add(pinned("B", .03));
        trip.stops.add(pinned("C", .01));
        trip.stops.add(pinned("D", .02));
        return trip;
    }

    private static String order(List<RoutePreview.Slot> slots) {
        StringBuilder sb = new StringBuilder();
        for (RoutePreview.Slot slot : slots) {
            sb.append(slot.stop.id);
        }
        return sb.toString();
    }

    // ---- ported RouteMath intents ----

    @Test public void optimize_visitsNearestNeighbourFirst() {
        assertEquals("ACDB", order(RoutePreview.day(lineTrip(), 1, true)));
    }

    @Test public void optimize_withoutOptimizeKeepsListOrder() {
        assertEquals("ABCD", order(RoutePreview.day(lineTrip(), 1, false)));
    }

    @Test public void optimize_reducesTotalTravel() {
        DraftTrip trip = lineTrip();
        int current = RoutePreview.totalTravel(RoutePreview.day(trip, 1, false));
        int preview = RoutePreview.totalTravel(RoutePreview.day(trip, 1, true));
        assertTrue(preview < current);
    }

    @Test public void optimize_bookedStopKeepsDayAndTimeAndFlexibleStopsFitAround() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("A", 1, 30, 15));
        DraftTrip.Stop lunch = booked("Lunch", 1, 660, 60);
        lunch.buffer = 15;
        trip.stops.add(lunch);
        trip.stops.add(stop("B", 1, 30, 15));
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, 1, true);
        assertEquals("ALunchB", order(slots));
        assertEquals(660, slots.get(1).start);
        assertTrue(slots.get(0).end <= slots.get(1).start);
        assertTrue(slots.get(2).start >= slots.get(1).end);
        assertEquals(1, trip.stops.get(1).day);
    }

    @Test public void day_returnsEveryStopOnceWithDistinctIds() {
        DraftTrip trip = lineTrip();
        trip.stops.add(booked("Dinner", 1, 1080, 90));
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, 1, true);
        Set<String> ids = new HashSet<>();
        for (RoutePreview.Slot slot : slots) {
            ids.add(slot.stop.id);
        }
        assertEquals(5, slots.size());
        assertEquals(5, ids.size());
    }

    @Test public void day_singleStopStartsAtNineWithNoTravel() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("A", 1, 60, 30));
        RoutePreview.Slot slot = RoutePreview.day(trip, 1, true).get(0);
        assertEquals(540, slot.start);
        assertEquals(0, slot.travel);
    }

    // ---- conflicts ----

    @Test public void overlappingBookingIsFlagged() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(booked("A", 1, 600, 120));
        trip.stops.add(booked("B", 1, 660, 60));
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, 1, false);
        assertFalse(slots.get(0).conflict);
        assertTrue(slots.get(1).conflict);
        assertEquals(660, slots.get(1).start);
    }

    @Test public void flexibleStopEndingAfterEightPmIsFlagged() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("Long", 1, 700, 0));
        assertTrue(RoutePreview.day(trip, 1, false).get(0).conflict);
    }

    @Test public void loneEarlyBookingIsNotFlagged() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(booked("Early", 1, 480, 60));
        assertFalse(RoutePreview.day(trip, 1, false).get(0).conflict);
    }

    @Test public void bookingPastMidnightIsFlagged() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(booked("Late", 1, 1400, 120));
        assertTrue(RoutePreview.day(trip, 1, false).get(0).conflict);
    }

    // ---- buffer, legs, modes ----

    @Test public void firstStopGetsNoBuffer() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("A", 1, 60, 45));
        assertEquals(540, RoutePreview.day(trip, 1, false).get(0).start);
    }

    @Test public void unpinnedEitherSideUsesTwentyMinutePlaceholderEvenWithExplicitMode() {
        DraftTrip.Stop located = pinned("L", 0);
        DraftTrip.Stop unpinned = stop("U", 1, 30, 0);
        unpinned.mode = "Walking";
        located.mode = "Walking";
        assertEquals(20, RoutePreview.travel(located, unpinned));
        assertEquals(20, RoutePreview.travel(unpinned, located));
        assertEquals(0, RoutePreview.travel(null, located));
    }

    @Test public void eachLegUsesItsOwnModeAndBuffer() {
        DraftTrip trip = new DraftTrip();
        DraftTrip.Stop a = pinned("A", 0), b = pinned("B", .05), c = pinned("C", .10);
        a.mode = "Suggested";
        b.mode = "Walking"; b.buffer = 10;
        c.mode = "Driving"; c.buffer = 0;
        trip.stops.add(a); trip.stops.add(b); trip.stops.add(c);
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, 1, false);
        assertEquals(slots.get(0).end + slots.get(1).travel + 10, slots.get(1).start);
        assertEquals(slots.get(1).end + slots.get(2).travel, slots.get(2).start);
        assertTrue(slots.get(2).travel < slots.get(1).travel);
    }

    @Test public void suggestedModeFollowsDistanceBands() {
        DraftTrip.Stop a = pinned("A", 0), near = pinned("N", .005), mid = pinned("M", .03);
        near.mode = mid.mode = "Suggested";
        assertEquals("Walking", RoutePreview.mode(a, near));
        assertEquals("Cycling", RoutePreview.mode(a, mid));
    }

    // ---- optimize with unpinned stops ----

    @Test public void optimize_unpinnedStopsKeepOriginalOrder() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("X", 1, 30, 0));
        trip.stops.add(stop("Y", 1, 30, 0));
        trip.stops.add(stop("Z", 1, 30, 0));
        assertEquals("XYZ", order(RoutePreview.day(trip, 1, true)));
    }

    @Test public void optimize_unpinnedStopsRankLastInOriginalOrder() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("U1", 1, 30, 0));
        trip.stops.add(pinned("A", 0));
        trip.stops.add(stop("U2", 1, 30, 0));
        trip.stops.add(pinned("B", .01));
        assertEquals("ABU1U2", order(RoutePreview.day(trip, 1, true)));
    }

    // ---- compare / totalTravel ----

    @Test public void compare_reportsBothTotalsAndOrderChange() {
        DraftTrip trip = lineTrip();
        RoutePreview.Comparison cmp = RoutePreview.compare(trip, 1);
        assertEquals("ABCD", order(cmp.current));
        assertEquals("ACDB", order(cmp.preview));
        assertEquals(RoutePreview.totalTravel(cmp.current), cmp.currentTravel);
        assertEquals(RoutePreview.totalTravel(cmp.preview), cmp.previewTravel);
        assertTrue(cmp.previewTravel < cmp.currentTravel);
        assertTrue(cmp.orderChanged);
    }

    @Test public void compare_singleStopDoesNotChangeOrder() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("A", 1, 30, 0));
        assertFalse(RoutePreview.compare(trip, 1).orderChanged);
    }

    @Test public void compare_emptyDayHasNoTravel() {
        RoutePreview.Comparison cmp = RoutePreview.compare(new DraftTrip(), 2);
        assertEquals(0, cmp.currentTravel);
        assertFalse(cmp.orderChanged);
    }

    @Test public void totalTravel_sumsLegMinutesAndHandlesEmpty() {
        assertEquals(0, RoutePreview.totalTravel(new ArrayList<RoutePreview.Slot>()));
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("A", 1, 30, 0));
        trip.stops.add(stop("B", 1, 30, 0));
        assertEquals(20, RoutePreview.totalTravel(RoutePreview.day(trip, 1, false)));
    }

    // ---- balance ----

    @Test public void balance_spreadsFlexibleStopsAndDoesNotMutateInput() {
        DraftTrip trip = new DraftTrip();
        trip.days = 3;
        for (int i = 0; i < 6; i++) {
            trip.stops.add(stop("s" + i, 1, 60, 15));
        }
        Map<String, Integer> result = RoutePreview.balance(trip);
        assertEquals(6, result.size());
        int[] perDay = new int[4];
        for (Integer day : result.values()) {
            perDay[day]++;
        }
        assertEquals(2, perDay[1]);
        assertEquals(2, perDay[2]);
        assertEquals(2, perDay[3]);
        for (DraftTrip.Stop s : trip.stops) {
            assertEquals(1, s.day);
        }
    }

    @Test public void balance_neverMovesBookedStopsAndLoadsAroundThem() {
        DraftTrip trip = new DraftTrip();
        trip.days = 2;
        trip.stops.add(booked("Big", 1, 600, 300));
        trip.stops.add(stop("a", 1, 60, 15));
        trip.stops.add(stop("b", 1, 60, 15));
        Map<String, Integer> result = RoutePreview.balance(trip);
        assertFalse(result.containsKey("Big"));
        assertEquals(Integer.valueOf(2), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test public void balance_ignoresBookedStopsOutsideTripDays() {
        DraftTrip trip = new DraftTrip();
        trip.days = 2;
        trip.stops.add(booked("Zero", 0, 600, 60));
        trip.stops.add(booked("Far", 9, 600, 60));
        trip.stops.add(stop("a", 1, 60, 15));
        Map<String, Integer> result = RoutePreview.balance(trip);
        assertEquals(1, result.size());
        assertTrue(result.get("a") >= 1 && result.get("a") <= 2);
    }

    @Test public void balance_emptyDaysReceiveStops() {
        DraftTrip trip = new DraftTrip();
        trip.days = 3;
        trip.stops.add(booked("B1", 1, 600, 60));
        trip.stops.add(stop("a", 1, 60, 15));
        trip.stops.add(stop("b", 1, 60, 15));
        Map<String, Integer> result = RoutePreview.balance(trip);
        assertEquals(Integer.valueOf(2), result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    @Test public void balance_noFlexibleStopsReturnsEmptyMap() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(booked("B", 1, 600, 60));
        assertTrue(RoutePreview.balance(trip).isEmpty());
        assertNull(RoutePreview.balance(trip).get("B"));
    }
}
