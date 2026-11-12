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
    public void format_usesSymbolAndThousandsSeparator() {
        assertEquals("₱1,234", CurrencyMock.format(1234.4, "PHP"));
        assertEquals("¥3,000", CurrencyMock.format(3000, "JPY"));
    }
}
