package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TripDatesTest {

    @Test
    public void label_spansStartToLastDay() {
        assertEquals("Dec 1 – Dec 3", TripDates.label("2026-12-01", 3));
    }

    @Test
    public void label_singleDayRepeatsNothing() {
        assertEquals("Dec 1", TripDates.label("2026-12-01", 1));
    }

    @Test
    public void label_crossesMonthAndYear() {
        assertEquals("Dec 30 – Jan 2", TripDates.label("2026-12-30", 4));
    }

    @Test
    public void label_invalidDateIsEmpty() {
        assertEquals("", TripDates.label("", 3));
        assertEquals("", TripDates.label("nope", 3));
    }

    @Test
    public void isoFromUtcMillis_usesUtcDay() {
        // 2026-12-01T00:00:00Z
        assertEquals("2026-12-01", TripDates.isoFromUtcMillis(1796083200000L));
    }

    @Test
    public void daysBetweenInclusive_countsBothEnds() {
        assertEquals(3, TripDates.daysBetweenInclusive(1796083200000L, 1796083200000L + 2 * 86400000L));
        assertEquals(1, TripDates.daysBetweenInclusive(1796083200000L, 1796083200000L));
    }

    @Test
    public void dayDateLabel_showsWeekdayMonthAndDay() {
        assertEquals("Tue, Dec 1", TripDates.dayDateLabel("2026-12-01", 1));
        assertEquals("Wed, Dec 2", TripDates.dayDateLabel("2026-12-01", 2));
    }

    @Test
    public void dayDateLabel_crossesMonthAndYearBoundaries() {
        assertEquals("Fri, Jan 1", TripDates.dayDateLabel("2026-12-30", 3));
    }

    @Test
    public void dayDateLabel_badDateOrDayIsEmpty() {
        assertEquals("", TripDates.dayDateLabel("", 1));
        assertEquals("", TripDates.dayDateLabel("2026-13-40", 1));
        assertEquals("", TripDates.dayDateLabel("2026-12-01", 0));
    }
}
