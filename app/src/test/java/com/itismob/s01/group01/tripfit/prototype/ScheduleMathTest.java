package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.List;

/** Clock helpers plus the flow expectations that used to live on ScheduleMath.build. */
public class ScheduleMathTest {

    private static TripSnapshot.Stop stop(String name, int duration, int buffer) {
        TripSnapshot.Stop s = new TripSnapshot.Stop();
        s.name = name;
        s.duration = duration;
        s.buffer = buffer;
        return s;
    }

    @Test
    public void flexibleStops_flowFromNineAmWithPlaceholderLegAndBuffer() {
        TripSnapshot trip = new TripSnapshot();
        trip.stops.add(stop("A", 45, 15));
        trip.stops.add(stop("B", 30, 15));
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, 1, false);
        assertEquals(9 * 60, slots.get(0).start);
        assertEquals(9 * 60 + 45, slots.get(0).end);
        assertEquals(585 + ScheduleMath.PLACEHOLDER_TRAVEL_MIN + 15, slots.get(1).start);
        assertEquals(10 * 60 + 20, slots.get(1).start);
        assertEquals(10 * 60 + 50, slots.get(1).end);
    }

    @Test
    public void bookedStop_isPinnedToItsFixedTimeWithoutConflict() {
        TripSnapshot trip = new TripSnapshot();
        trip.stops.add(stop("A", 30, 15));
        TripSnapshot.Stop dinner = stop("Dinner", 90, 15);
        dinner.booked = true;
        dinner.fixedMinute = 18 * 60;
        trip.stops.add(dinner);
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, 1, false);
        assertEquals(18 * 60, slots.get(1).start);
        assertEquals(19 * 60 + 30, slots.get(1).end);
        assertFalse(slots.get(1).conflict);
    }

    @Test
    public void constants_matchTheAgreedValues() {
        assertEquals(540, ScheduleMath.DAY_START_MIN);
        assertEquals(1200, ScheduleMath.LATE_END_MIN);
        assertEquals(1440, ScheduleMath.DAY_END_MIN);
        assertEquals(20, ScheduleMath.PLACEHOLDER_TRAVEL_MIN);
        assertEquals(15, ScheduleMath.DEFAULT_BUFFER_MIN);
        assertEquals(60, ScheduleMath.DEFAULT_STAY_MIN);
    }

    @Test
    public void format_padsHoursAndMinutes() {
        assertEquals("09:05", ScheduleMath.format(9 * 60 + 5));
        assertEquals("00:30", ScheduleMath.format(24 * 60 + 30));
    }

    @Test
    public void parse_readsHoursAndMinutes() {
        assertEquals(15 * 60 + 30, ScheduleMath.parse("15:30"));
    }
}
