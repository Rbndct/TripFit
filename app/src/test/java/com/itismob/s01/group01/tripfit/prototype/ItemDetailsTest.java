package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ItemDetailsTest {

    @Test
    public void withCost_firstTimeDefaultsToOutdoor() {
        ItemDetails details = ItemDetails.withCost(null, 120.5, "JPY", "Food");
        assertEquals(120.5, details.getCost(), 0.0);
        assertEquals("JPY", details.getCostCurrency());
        assertEquals("Food", details.getCategory());
        assertTrue(details.isOutdoor());
    }

    @Test
    public void withCost_keepsTheOutdoorFlagAndDoesNotMutateThePrevious() {
        ItemDetails previous = new ItemDetails(1, "PHP", "Other", false);
        ItemDetails updated = ItemDetails.withCost(previous, 9, "JPY", "Food");
        assertFalse(updated.isOutdoor());
        assertEquals(9.0, updated.getCost(), 0.0);
        assertEquals(1.0, previous.getCost(), 0.0);
        assertEquals("PHP", previous.getCostCurrency());
    }
}
