package com.itismob.s01.group01.tripfit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.remote.dto.RatesDto;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/** Exercises {@link FrankfurterDataSource} against a fake client (no live network). */
public class FrankfurterDataSourceTest {

    @Test
    public void fetchCurrenciesSync_parsesCodeToNameMap() throws Exception {
        FrankfurterDataSource source = new FrankfurterDataSource(new FakeFrankfurterClient().client());

        Map<String, String> currencies = source.fetchCurrenciesSync();

        assertEquals(4, currencies.size());
        assertEquals("Philippine Peso", currencies.get("PHP"));
        assertEquals("Japanese Yen", currencies.get("JPY"));
    }

    @Test
    public void fetchUsdRatesSync_parsesBaseDateAndRates() throws Exception {
        FrankfurterDataSource source = new FrankfurterDataSource(new FakeFrankfurterClient().client());

        RatesDto rates = source.fetchUsdRatesSync();

        assertEquals("USD", rates.getBase());
        assertEquals("2026-11-13", rates.getDate());
        assertEquals(160.0, rates.getRates().get("JPY"), 0.0001);
        assertEquals(64.0, rates.getRates().get("PHP"), 0.0001);
    }

    @Test
    public void fetchUsdRatesSync_asksForUsdAsTheBase() throws Exception {
        FakeFrankfurterClient fake = new FakeFrankfurterClient();

        new FrankfurterDataSource(fake.client()).fetchUsdRatesSync();

        assertEquals(1, fake.requests().size());
        assertEquals("USD", fake.requests().get(0).url().queryParameter("base"));
        assertTrue(fake.requests().get(0).url().encodedPath().endsWith("/latest"));
    }

    @Test(expected = IOException.class)
    public void fetchCurrenciesSync_throwsOnHttpError() throws Exception {
        new FrankfurterDataSource(FakeFrankfurterClient.failingWith(500).client()).fetchCurrenciesSync();
    }

    @Test(expected = IOException.class)
    public void fetchUsdRatesSync_throwsOnNotFound() throws Exception {
        new FrankfurterDataSource(FakeFrankfurterClient.failingWith(404).client()).fetchUsdRatesSync();
    }

    @Test(expected = IOException.class)
    public void fetchUsdRatesSync_throwsWhenResponseHasNoRates() throws Exception {
        FakeFrankfurterClient fake = new FakeFrankfurterClient(200, "{}", "{\"base\":\"USD\"}");

        new FrankfurterDataSource(fake.client()).fetchUsdRatesSync();
    }
}
