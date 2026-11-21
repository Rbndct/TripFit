package com.itismob.s01.group01.tripfit.prototype;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;

public class TripExportTest {
    @Test public void calendarUsesDestinationTimezoneAndEscapesPlaceNames() {
        TripSnapshot trip=new TripSnapshot(); trip.startDate="2026-12-01"; trip.city="Tokyo";
        TripSnapshot.Stop stop=new TripSnapshot.Stop(); stop.name="Museum, gallery; art"; trip.stops.add(stop);
        String calendar=TripExport.calendar(trip);
        assertTrue(calendar.contains("DTSTART:20261201T000000Z"));
        assertTrue(calendar.contains("DTEND:20261201T010000Z"));
        assertTrue(calendar.contains("SUMMARY:Museum\\, gallery\\; art"));
        assertEquals(1,calendar.split("BEGIN:VEVENT",-1).length-1);
    }
    @Test public void calendarFoldsUnicodeWithoutBreakingUtf8Lines() {
        TripSnapshot trip=new TripSnapshot(); TripSnapshot.Stop stop=new TripSnapshot.Stop();
        StringBuilder title=new StringBuilder(); for(int i=0;i<80;i++) title.append("東京"); stop.name=title.toString(); trip.stops.add(stop);
        String calendar=TripExport.calendar(trip);
        for(String line:calendar.split("\r\n")) assertTrue(line.getBytes(StandardCharsets.UTF_8).length<=75);
        assertTrue(calendar.replace("\r\n ","").contains("SUMMARY:"+stop.name));
    }
    @Test public void changingCityDoesNotChangeAnExistingStopsPriceCurrency() {
        TripSnapshot trip=new TripSnapshot(); TripSnapshot.Stop stop=DemoCatalog.places("Tokyo").get(1).stop(1);
        double original=trip.convertedCost(stop); trip.city="Manila";
        assertEquals(original,trip.convertedCost(stop),.001);
    }
    @Test public void summaryKeepsEstimatesAndExpensesSeparate() {
        TripSnapshot trip=new TripSnapshot(); trip.budget=1000;
        TripSnapshot.Stop stop=new TripSnapshot.Stop(); stop.cost=100; stop.currency="PHP"; trip.stops.add(stop);
        TripSnapshot.Expense expense=new TripSnapshot.Expense(); expense.amount=50; trip.expenses.add(expense);
        String summary=TripExport.summary(trip);
        assertTrue(summary.contains("Estimated stops: 100"));
        assertTrue(summary.contains("Logged expenses: 50"));
        assertTrue(summary.contains("Remaining after expenses: 950"));
    }
    @Test public void calendarRejectsMissingOrMalformedDates() {
        TripSnapshot trip = new TripSnapshot();
        trip.stops.add(new TripSnapshot.Stop());
        for (String date : new String[]{null, "", "2026-02-30", "2026-12-01extra"}) {
            trip.startDate = date;
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> TripExport.calendar(trip));
            assertEquals("Invalid trip date", error.getMessage());
        }
    }

    @Test public void calendarUsesLocalWallTimeOnDaylightSavingTransition() {
        TripSnapshot trip = new TripSnapshot();
        trip.startDate = "2026-03-08";
        trip.city = "New York";
        TripSnapshot.Stop stop = new TripSnapshot.Stop();
        stop.name = "Gallery";
        stop.address = "1 Main St, New York";
        trip.stops.add(stop);

        String calendar = TripExport.calendar(trip);
        assertTrue(calendar.contains("DTSTART:20260308T130000Z"));
        assertTrue(calendar.contains("LOCATION:1 Main St\\, New York"));
    }

    @Test public void calendarKeepsDurationWhenBookedTimeFallsInSpringForwardGap() {
        TripSnapshot trip = new TripSnapshot();
        trip.startDate = "2026-03-08";
        trip.city = "New York";
        TripSnapshot.Stop stop = new TripSnapshot.Stop();
        stop.booked = true;
        stop.fixedMinute = 165; // 02:45 local is skipped when clocks advance.
        stop.duration = 30;
        trip.stops.add(stop);

        String calendar = TripExport.calendar(trip);
        assertTrue(calendar.contains("DTSTART:20260308T074500Z"));
        assertTrue(calendar.contains("DTEND:20260308T081500Z"));
    }

    @Test public void calendarUsesPinBeforeFallingBackToCity() {
        TripSnapshot trip = new TripSnapshot();
        trip.startDate = "2026-12-01";
        trip.city = "Manila";
        TripSnapshot.Stop pinned = new TripSnapshot.Stop();
        pinned.hasLocation = true;
        pinned.lat = 14.5995;
        pinned.lng = 120.9842;
        trip.stops.add(pinned);
        trip.stops.add(new TripSnapshot.Stop());

        String calendar = TripExport.calendar(trip);
        assertTrue(calendar.contains("DTSTART:20261201T010000Z"));
        assertTrue(calendar.contains("LOCATION:14.59950\\, 120.98420"));
        assertTrue(calendar.contains("LOCATION:Manila"));
    }
}
