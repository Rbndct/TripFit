package com.itismob.s01.group01.tripfit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import com.itismob.s01.group01.tripfit.prototype.TravelMode;

import org.junit.Test;

import java.util.List;

public class ItineraryRowTest {

    private static DraftTrip.Stop stop(String id, int duration, int buffer) {
        DraftTrip.Stop s = new DraftTrip.Stop();
        s.id = id;
        s.name = "Name " + id;
        s.duration = duration;
        s.buffer = buffer;
        return s;
    }

    private static DraftTrip.Stop pinned(String id, double lng) {
        DraftTrip.Stop s = stop(id, 30, 10);
        s.hasLocation = true;
        s.lat = 35.0;
        s.lng = lng;
        return s;
    }

    @Test
    public void rows_followSlotOrderAroundBookedStops() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("a", 30, 15));
        DraftTrip.Stop lunch = stop("lunch", 60, 15);
        lunch.booked = true;
        lunch.fixedMinute = 660;
        trip.stops.add(lunch);
        trip.stops.add(stop("b", 30, 15));
        List<ItineraryRow> rows = ItineraryRow.build(trip, 1);
        assertEquals(3, rows.size());
        assertEquals("a", rows.get(0).getId());
        assertEquals("lunch", rows.get(1).getId());
        assertEquals(660, rows.get(1).getStart());
        assertTrue(rows.get(1).isBooked());
        assertEquals(1, rows.get(0).getIndexInDay());
        assertEquals(3, rows.get(2).getIndexInDay());
    }

    @Test
    public void firstRow_hasNoIncomingLeg() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("a", 30, 25));
        ItineraryRow row = ItineraryRow.build(trip, 1).get(0);
        assertFalse(row.hasIncomingLeg());
        assertEquals(0, row.getIncomingTravel());
        assertEquals(540, row.getStart());
    }

    @Test
    public void unpinnedEndOfLeg_usesPlaceholderAndPinnedRowExposesCoordinates() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(pinned("a", 139.70));
        trip.stops.add(stop("b", 30, 10));
        List<ItineraryRow> rows = ItineraryRow.build(trip, 1);
        assertTrue(rows.get(0).hasLocation());
        assertEquals(139.70, rows.get(0).getLng(), 1e-9);
        assertFalse(rows.get(1).hasLocation());
        assertTrue(rows.get(1).isPlaceholderLeg());
        assertEquals(20, rows.get(1).getIncomingTravel());
        assertEquals(10, rows.get(1).getBuffer());
    }

    @Test
    public void legMode_explicitWinsAndSuggestedResolvesByDistance() {
        DraftTrip trip = new DraftTrip();
        DraftTrip.Stop a = pinned("a", 139.70);
        DraftTrip.Stop b = pinned("b", 139.80);
        DraftTrip.Stop c = pinned("c", 139.90);
        b.mode = "Driving";
        trip.stops.add(a);
        trip.stops.add(b);
        trip.stops.add(c);
        List<ItineraryRow> rows = ItineraryRow.build(trip, 1);
        assertNull(rows.get(0).getExplicitMode());
        assertEquals(TravelMode.DRIVE, rows.get(1).getExplicitMode());
        assertEquals(TravelMode.DRIVE, rows.get(1).getLegMode());
        assertNull(rows.get(2).getExplicitMode());
        assertEquals(TravelMode.TRANSIT, rows.get(2).getLegMode());
    }

    @Test
    public void conflict_isCarriedFromTheEngine() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("long", 700, 0));
        assertTrue(ItineraryRow.build(trip, 1).get(0).isConflict());
    }

    @Test
    public void totals_sumStayAndTravelMinutes() {
        DraftTrip trip = new DraftTrip();
        trip.stops.add(stop("a", 30, 0));
        trip.stops.add(stop("b", 45, 0));
        List<ItineraryRow> rows = ItineraryRow.build(trip, 1);
        assertEquals(75, ItineraryRow.stayMinutes(rows));
        assertEquals(20, ItineraryRow.travelMinutes(rows));
        assertEquals(0, ItineraryRow.stayMinutes(ItineraryRow.build(trip, 2)));
    }
}
