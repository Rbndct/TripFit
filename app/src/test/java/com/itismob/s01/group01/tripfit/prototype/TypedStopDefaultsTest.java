package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/** P-119: a typed (non-catalog) stop starts with the documented defaults. */
public class TypedStopDefaultsTest {

    private PrototypeStore store;
    private final TripMeta meta = new TripMeta("Tokyo", "", 2, "PHP");

    @Before
    public void setUp() {
        store = PrototypeStore.forStorage(new InMemoryKeyValueStorage());
    }

    private TripSnapshot typedTrip() {
        ItineraryItemWithPlace row = new ItineraryItemWithPlace();
        row.item = new ItineraryItemEntity("typed", "t", null, "My spot", 1, null, null, false,
                null, null, 0);
        return TripSnapshots.from("t", meta, Collections.singletonList(row), store);
    }

    private TripSnapshot.Stop typedStop() {
        return typedTrip().stops.get(0);
    }

    @Test
    public void withoutStoredDetails_categoryIsActivities_costZeroInLocalCurrency() {
        TripSnapshot trip = typedTrip();
        TripSnapshot.Stop stop = trip.stops.get(0);
        assertEquals("Activities", stop.category);
        assertEquals(0.0, stop.cost, 0.0);
        assertEquals("JPY", trip.costCurrency(stop)); // blank currency falls back to local
        assertTrue(stop.outdoor);
    }

    @Test
    public void formDefaults_giveActivitiesZeroLocalOutdoor() {
        // What the Add Destination cost block stores for an untouched typed stop.
        store.setItemDetails("typed",
                ItemDetails.withCost(null, 0, MockData.currencyFor("Tokyo"), "Activities"));
        TripSnapshot.Stop stop = typedStop();
        assertEquals("Activities", stop.category);
        assertEquals(0.0, stop.cost, 0.0);
        assertEquals("JPY", stop.currency);
        assertTrue(stop.outdoor);
    }

    @Test
    public void bufferIsNull_andFollowsProfileDefault() {
        store.setBufferMin(25);
        TripSnapshot.Stop stop = typedStop();
        assertNull(stop.explicitBuffer);
        assertEquals(25, stop.buffer);
    }

    @Test
    public void modeIsNull_meaningSuggested() {
        TripSnapshot.Stop stop = typedStop();
        assertEquals("Suggested", stop.mode);
        assertNull(TripSnapshots.toMode(stop.mode));
    }

    @Test
    public void withCost_nullPrevious_defaultsOutdoorOn() {
        ItemDetails details = ItemDetails.withCost(null, 0, "JPY", "Activities");
        assertTrue(details.isOutdoor());
        assertEquals("Activities", details.getCategory());
        assertEquals(0.0, details.getCost(), 0.0);
    }
}
