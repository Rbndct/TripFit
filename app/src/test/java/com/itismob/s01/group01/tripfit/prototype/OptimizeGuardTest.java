package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Random;

/** Ported from Kevin's ProposalRulesTest: an optimized preview must never make a day worse. */
public class OptimizeGuardTest {

    private static TripSnapshot.Stop stop(String id, double lat, double lng, boolean pinned) {
        TripSnapshot.Stop s = new TripSnapshot.Stop();
        s.id = id;
        s.name = id;
        s.day = 1;
        s.duration = 30;
        s.hasLocation = pinned;
        s.lat = lat;
        s.lng = lng;
        s.mode = "Driving";
        return s;
    }

    private static int conflicts(List<RoutePreview.Slot> slots) {
        int count = 0;
        for (RoutePreview.Slot slot : slots) {
            if (slot.conflict) count++;
        }
        return count;
    }

    private static TripSnapshot randomTrip(Random random) {
        TripSnapshot trip = new TripSnapshot();
        int size = 3 + random.nextInt(5);
        for (int i = 0; i < size; i++) {
            boolean pinned = random.nextInt(4) != 0;
            trip.stops.add(stop("S" + i, random.nextDouble() * .2, random.nextDouble() * .2, pinned));
        }
        if (random.nextBoolean()) {
            TripSnapshot.Stop booked = stop("B", .1, .1, true);
            booked.booked = true;
            booked.fixedMinute = 12 * 60 + random.nextInt(180);
            trip.stops.add(booked);
        }
        return trip;
    }

    @Test
    public void previewNeverTravelsLongerOrConflictsMoreThanCurrentOrder() {
        Random random = new Random(42);
        for (int i = 0; i < 500; i++) {
            TripSnapshot trip = randomTrip(random);
            RoutePreview.Comparison comparison = RoutePreview.compare(trip, 1);
            assertTrue("trip " + i + " travel " + comparison.previewTravel + " > " + comparison.currentTravel,
                    comparison.previewTravel <= comparison.currentTravel);
            assertTrue("trip " + i + " conflicts",
                    conflicts(comparison.preview) <= conflicts(comparison.current));
        }
    }

    @Test
    public void whenPreviewIsNoBetterTheCurrentOrderIsKept() {
        Random random = new Random(7);
        for (int i = 0; i < 500; i++) {
            TripSnapshot trip = randomTrip(random);
            List<RoutePreview.Slot> current = RoutePreview.day(trip, 1, false);
            List<RoutePreview.Slot> applied = RoutePreview.day(trip, 1, true);
            if (RoutePreview.totalTravel(applied) == RoutePreview.totalTravel(current)
                    && conflicts(applied) == conflicts(current)) {
                continue;
            }
            assertTrue(RoutePreview.totalTravel(applied) < RoutePreview.totalTravel(current)
                    || conflicts(applied) < conflicts(current));
        }
    }

    @Test
    public void optimizingDoesNotMutateTheTripOrMoveBookedStops() {
        TripSnapshot trip = randomTrip(new Random(1));
        TripSnapshot.Stop booked = stop("FIXED", .05, .05, true);
        booked.booked = true;
        booked.fixedMinute = 14 * 60;
        trip.stops.add(booked);
        String before = trip.stops.toString();
        RoutePreview.compare(trip, 1);
        assertEquals(before, trip.stops.toString());
        for (RoutePreview.Slot slot : RoutePreview.day(trip, 1, true)) {
            if (slot.stop == booked) {
                assertEquals(14 * 60, slot.start);
                return;
            }
        }
        assertFalse("booked stop missing from day", true);
    }
}
