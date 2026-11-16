package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The exported summary shows every amount to exactly two decimals. */
public class TripExportAmountsTest {

    private static DraftTrip trip() {
        DraftTrip trip = new DraftTrip();
        trip.name = "Test trip";
        trip.city = "Tokyo";          // local currency JPY
        trip.days = 1;
        trip.homeCurrency = "PHP";
        trip.budget = 15000;
        DraftTrip.Stop stop = new DraftTrip.Stop();
        stop.name = "Senso-ji";
        stop.category = "Activities";
        stop.cost = 1234.5;           // JPY, converted at the prototype's sample rate
        trip.stops.add(stop);
        return trip;
    }

    @Test
    public void summary_showsStopEstimateWithTwoDecimals() {
        String text = TripExport.summary(trip());

        assertTrue(text, text.contains("Estimate: JPY 1234.50 (≈ PHP "));
    }

    @Test
    public void summary_showsBudgetLinesWithTwoDecimals() {
        String text = TripExport.summary(trip());

        assertTrue(text, text.contains("Trip limit: 15000.00"));
        assertTrue(text, text.contains("Logged expenses: 0.00"));
    }

    @Test
    public void summary_showsCategoryLinesWithTwoDecimals() {
        String text = TripExport.summary(trip());

        assertTrue(text, text.contains("Food: limit 4500.00 / planned 0.00 / logged 0.00"));
        assertTrue(text, text.contains("Activities: limit 6000.00 / planned "));
    }

    @Test
    public void summary_hasNoWholeNumberAmountsLeft() {
        String text = TripExport.summary(trip());

        assertFalse(text, text.contains("Trip limit: 15000\n"));
        assertFalse(text, text.contains("logged 0\n"));
    }
}
