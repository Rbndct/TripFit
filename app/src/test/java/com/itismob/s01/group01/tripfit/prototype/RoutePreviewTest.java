package com.itismob.s01.group01.tripfit.prototype;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class RoutePreviewTest {
    @Test public void fixedBookingsKeepTheirTimeAndFlexibleStopsFitAroundThem() {
        DraftTrip trip=new DraftTrip();
        DraftTrip.Stop longVisit=new DraftTrip.Stop(); longVisit.name="Long visit"; longVisit.duration=180;
        DraftTrip.Stop booking=new DraftTrip.Stop(); booking.booked=true; booking.fixedMinute=600; booking.duration=60;
        trip.stops.add(longVisit); trip.stops.add(booking);
        List<RoutePreview.Slot> slots=RoutePreview.day(trip,1,true);
        assertSame(booking,slots.get(0).stop); assertEquals(600,slots.get(0).start);
        assertSame(longVisit,slots.get(1).stop); assertTrue(slots.get(1).start>=slots.get(0).end);
    }
    @Test public void overlappingBookingsAreFlaggedWithoutMovingTheirTimes() {
        DraftTrip trip=new DraftTrip();
        for(int i=0;i<2;i++) { DraftTrip.Stop stop=new DraftTrip.Stop(); stop.booked=true; stop.fixedMinute=600+i*10; trip.stops.add(stop); }
        List<RoutePreview.Slot> slots=RoutePreview.day(trip,1,false);
        assertEquals(610,slots.get(1).start); assertTrue(slots.get(1).conflict);
    }
    @Test public void perLegModeAndBufferAffectArrival() {
        DraftTrip trip=new DraftTrip();
        DraftTrip.Stop a=new DraftTrip.Stop(), b=new DraftTrip.Stop();
        a.hasLocation=b.hasLocation=true; a.lat=14.59; b.lat=14.60; a.lng=b.lng=120.98;
        b.mode="Walking"; b.buffer=25; trip.stops.add(a); trip.stops.add(b);
        List<RoutePreview.Slot> slots=RoutePreview.day(trip,1,false);
        int walking=slots.get(1).travel; assertEquals(slots.get(0).end+walking+25,slots.get(1).start);
        b.mode="Driving"; assertTrue(RoutePreview.travel(a,b)<walking);
    }
    @Test public void currencyAndDailyBudgetAreConsistent() {
        assertEquals(380,RoutePreview.convert(1000,"JPY","PHP"),.001);
        assertEquals(1000,RoutePreview.convert(380,"PHP","JPY"),.001);
        DraftTrip trip=new DraftTrip(); trip.budget=100; trip.days=3; trip.dailyBudget=true;
        assertEquals(300,trip.totalBudget(),.001);
    }
}
