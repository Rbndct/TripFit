package com.itismob.s01.group01.tripfit.prototype;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;

public class TripExportTest {
    @Test public void calendarUsesDestinationTimezoneAndEscapesPlaceNames() {
        DraftTrip trip=new DraftTrip(); trip.startDate="2026-12-01"; trip.city="Tokyo";
        DraftTrip.Stop stop=new DraftTrip.Stop(); stop.name="Museum, gallery; art"; trip.stops.add(stop);
        String calendar=TripExport.calendar(trip);
        assertTrue(calendar.contains("DTSTART:20261201T000000Z"));
        assertTrue(calendar.contains("DTEND:20261201T010000Z"));
        assertTrue(calendar.contains("SUMMARY:Museum\\, gallery\\; art"));
        assertEquals(1,calendar.split("BEGIN:VEVENT",-1).length-1);
    }
    @Test public void calendarFoldsUnicodeWithoutBreakingUtf8Lines() {
        DraftTrip trip=new DraftTrip(); DraftTrip.Stop stop=new DraftTrip.Stop();
        StringBuilder title=new StringBuilder(); for(int i=0;i<80;i++) title.append("東京"); stop.name=title.toString(); trip.stops.add(stop);
        String calendar=TripExport.calendar(trip);
        for(String line:calendar.split("\r\n")) assertTrue(line.getBytes(StandardCharsets.UTF_8).length<=75);
        assertTrue(calendar.replace("\r\n ","").contains("SUMMARY:"+stop.name));
    }
    @Test public void changingCityDoesNotChangeAnExistingStopsPriceCurrency() {
        DraftTrip trip=new DraftTrip(); DraftTrip.Stop stop=DemoCatalog.places("Tokyo").get(1).stop(1);
        double original=trip.convertedCost(stop); trip.city="Manila";
        assertEquals(original,trip.convertedCost(stop),.001);
    }
    @Test public void summaryKeepsEstimatesAndExpensesSeparate() {
        DraftTrip trip=new DraftTrip(); trip.budget=1000;
        DraftTrip.Stop stop=new DraftTrip.Stop(); stop.cost=100; stop.currency="PHP"; trip.stops.add(stop);
        DraftTrip.Expense expense=new DraftTrip.Expense(); expense.amount=50; trip.expenses.add(expense);
        String summary=TripExport.summary(trip);
        assertTrue(summary.contains("Estimated stops: 100"));
        assertTrue(summary.contains("Logged expenses: 50"));
        assertTrue(summary.contains("Remaining after expenses: 950"));
    }
}
