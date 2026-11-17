package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TripExportRatesTest {

    @Before
    public void sampleRates() {
        CurrencyRates.useFallback();
    }

    @After
    public void resetRates() {
        CurrencyRates.useFallback();
    }

    @Test
    public void footer_sampleRatesComeFromTheSingleTable() {
        String text = TripExport.summary(new DraftTrip());

        assertFalse(text, text.contains("0.38"));
        assertTrue(text, text.contains("1 USD = 56.0000 PHP"));
        assertTrue(text, text.contains("sample"));
    }

    @Test
    public void footer_liveRatesCarryTheirDate() {
        Map<String, Double> perUsd = new HashMap<>();
        perUsd.put("USD", 1.0);
        perUsd.put("PHP", 60.0);
        perUsd.put("JPY", 120.0);
        CurrencyRates.update(CurrencyRates.Snapshot.of(perUsd, null, "2026-11-14", true));

        String text = TripExport.summary(new DraftTrip());

        assertTrue(text, text.contains("as of 2026-11-14"));
        assertTrue(text, text.contains("1 USD = 60.0000 PHP"));
        assertFalse(text, text.contains("not live"));
    }
}
