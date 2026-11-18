package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class CatalogSuggestionsTest {
    @Test
    public void searchesOneCityIgnoringCaseAndReturnsAllForBlankQuery() {
        assertEquals("Nezu Shrine", DemoCatalog.search("Tokyo", " NEZU SHRINE ").get(0).name);
        assertTrue(DemoCatalog.search("Manila", "Nezu").isEmpty());
        assertEquals(DemoCatalog.places("Manila").size(), DemoCatalog.search("Manila", "").size());
    }

    @Test
    public void famousAndSuggestedPicksExcludeFoodAndPlannedStops() {
        assertFalse(CatalogSuggestions.Filter.FAMOUS.matches(
                DemoCatalog.search("Manila", "Intramuros lunch").get(0)));
        List<DemoCatalog.Place> picks = CatalogSuggestions.suggested("Manila",
                Set.of(DemoCatalog.nameKey(" FORT SANTIAGO ")));
        assertEquals(5, picks.size());
        assertEquals(2, picks.stream().filter(place -> place.hidden).count());
        assertTrue(picks.stream().noneMatch(place ->
                place.category.equals("Food") || place.name.equals("Fort Santiago")));
    }

    @Test
    public void suggestedDaysStartAtTargetAndCapAtTripEnd() {
        assertEquals(2, CatalogSuggestions.dayForPick(2, 3, 2, true));
        assertEquals(3, CatalogSuggestions.dayForPick(2, 3, 3, true));
        assertEquals(3, CatalogSuggestions.dayForPick(3, 3, 4, true));
        assertEquals(2, CatalogSuggestions.dayForPick(2, 3, 4, false));
    }

    @Test
    public void remainingUsesGreaterOfPlannedAndLogged() {
        assertEquals(300, CatalogSuggestions.remaining(1000, 700, 400), 0.0);
        assertEquals(-25, CatalogSuggestions.remaining(1000, 400, 1025), 0.0);
    }

    @Test
    public void nearbyUsesLastPinnedStopOnTargetDay() {
        DemoCatalog.Place temple = DemoCatalog.findById("offline-senso-ji");
        DemoCatalog.Place shrine = DemoCatalog.findById("offline-meiji");
        CatalogSuggestions.Anchor anchor = CatalogSuggestions.anchor("Tokyo", 2, List.of(
                stop(shrine, 1, 5), stop(temple, 2, 3), stop(shrine, 2, 1),
                stop(null, 2, 4)));
        assertEquals(temple.name, anchor.name);
        assertEquals(0, anchor.distance(temple), 0.000001);
        assertEquals(temple, CatalogSuggestions.nearby("Tokyo", "",
                CatalogSuggestions.Filter.ALL, anchor).get(0));
    }

    private static ItineraryItemWithPlace stop(DemoCatalog.Place place, int day, int position) {
        ItineraryItemWithPlace row = new ItineraryItemWithPlace();
        row.item = new ItineraryItemEntity("item", "trip", null, "Stop", day,
                null, null, false, null, null, position);
        if (place != null) {
            row.placeName = place.name;
            row.placeLat = place.lat;
            row.placeLng = place.lng;
        }
        return row;
    }

}
