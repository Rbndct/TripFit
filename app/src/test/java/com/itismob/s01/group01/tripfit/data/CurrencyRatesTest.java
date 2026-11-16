package com.itismob.s01.group01.tripfit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurrencyRatesTest {

    @After
    public void restoreFallback() {
        CurrencyRates.useFallback();
    }

    @Test
    public void default_isTheBuiltInFallbackTable() {
        assertFalse(CurrencyRates.isLive());
        assertTrue(CurrencyRates.codes().containsAll(Arrays.asList("PHP", "USD", "JPY", "EUR", "KRW", "SGD")));
        assertEquals(56.0, CurrencyRates.convert(150, "JPY", "PHP"), 0.0001);
    }

    @Test
    public void update_switchesConversionToTheLiveCrossRates() {
        CurrencyRates.update(liveSnapshot());

        // 160 JPY = 1 USD = 64 PHP in the live table
        assertEquals(64.0, CurrencyRates.convert(160, "JPY", "PHP"), 0.0001);
        assertTrue(CurrencyRates.isLive());
        assertEquals("2026-11-13", CurrencyRates.asOfDate());
    }

    @Test
    public void codes_comeFromTheLiveTableSortedAlphabetically() {
        CurrencyRates.update(liveSnapshot());

        List<String> codes = CurrencyRates.codes();

        assertEquals(Arrays.asList("EUR", "JPY", "PHP", "USD"), codes);
    }

    @Test
    public void nameOf_returnsTheLiveNameOrFallsBackToTheCode() {
        CurrencyRates.update(liveSnapshot());

        assertEquals("Philippine Peso", CurrencyRates.nameOf("PHP"));
        assertEquals("XXX", CurrencyRates.nameOf("XXX"));
    }

    @Test
    public void convert_sameCurrencyAndUnknownCodesPassThrough() {
        CurrencyRates.update(liveSnapshot());

        assertEquals(1000.0, CurrencyRates.convert(1000, "PHP", "PHP"), 0.0001);
        assertEquals(42.0, CurrencyRates.convert(42, "XXX", "PHP"), 0.0001);
        assertEquals(42.0, CurrencyRates.convert(42, "PHP", "XXX"), 0.0001);
    }

    @Test
    public void format_usesTheKnownSymbolOrTheCode() {
        CurrencyRates.update(liveSnapshot());

        assertEquals("₱1,234.40", CurrencyRates.format(1234.4, "PHP"));
        assertEquals("CHF 3,000.00", CurrencyRates.format(3000, "CHF"));
    }

    @Test
    public void useFallback_undoesAnUpdate() {
        CurrencyRates.update(liveSnapshot());

        CurrencyRates.useFallback();

        assertFalse(CurrencyRates.isLive());
        assertEquals(56.0, CurrencyRates.convert(150, "JPY", "PHP"), 0.0001);
    }

    @Test
    public void snapshot_isImmuneToLaterChangesInTheMapsItWasBuiltFrom() {
        Map<String, Double> perUsd = new HashMap<>();
        perUsd.put("USD", 1.0);
        perUsd.put("PHP", 64.0);
        CurrencyRates.update(CurrencyRates.Snapshot.of(perUsd, new HashMap<>(), "2026-11-13", true));

        perUsd.put("PHP", 1.0);

        assertEquals(64.0, CurrencyRates.convert(1, "USD", "PHP"), 0.0001);
    }

    private static CurrencyRates.Snapshot liveSnapshot() {
        Map<String, Double> perUsd = new HashMap<>();
        perUsd.put("USD", 1.0);
        perUsd.put("EUR", 0.9);
        perUsd.put("JPY", 160.0);
        perUsd.put("PHP", 64.0);
        Map<String, String> names = new HashMap<>();
        names.put("PHP", "Philippine Peso");
        names.put("JPY", "Japanese Yen");
        names.put("USD", "United States Dollar");
        names.put("EUR", "Euro");
        return CurrencyRates.Snapshot.of(perUsd, names, "2026-11-13", true);
    }
}
