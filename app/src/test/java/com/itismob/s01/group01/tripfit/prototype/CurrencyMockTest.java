package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CurrencyMockTest {

    @Test
    public void convert_sameCurrency_returnsSameAmount() {
        assertEquals(1000.0, CurrencyMock.convert(1000, "PHP", "PHP"), 0.0001);
    }

    @Test
    public void convert_jpyToPhp_usesUsdCrossRate() {
        // 150 JPY = 1 USD = 56 PHP
        assertEquals(56.0, CurrencyMock.convert(150, "JPY", "PHP"), 0.0001);
    }

    @Test
    public void convert_unknownCurrency_passesAmountThrough() {
        assertEquals(42.0, CurrencyMock.convert(42, "XXX", "PHP"), 0.0001);
    }

    @Test
    public void format_usesSymbolThousandsSeparatorAndTwoDecimals() {
        assertEquals("₱1,234.40", CurrencyMock.format(1234.4, "PHP"));
        assertEquals("¥3,000.00", CurrencyMock.format(3000, "JPY"));
    }

    @Test
    public void format_alwaysShowsExactlyTwoDecimals() {
        assertEquals("₱0.00", CurrencyMock.format(0, "PHP"));
        assertEquals("$1,234.50", CurrencyMock.format(1234.5, "USD"));
        assertEquals("€0.99", CurrencyMock.format(0.994, "EUR"));
        assertEquals("€1.00", CurrencyMock.format(0.996, "EUR"));
    }

    @Test
    public void format_roundsLongConvertedAmountsToTwoDecimals() {
        // 1200 JPY at the demo rates is 448.0, but a live cross rate gives long fractions.
        assertEquals("₱478.60", CurrencyMock.format(478.5984375, "PHP"));
    }
}
