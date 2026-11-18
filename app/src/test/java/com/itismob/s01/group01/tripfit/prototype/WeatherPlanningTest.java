package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class WeatherPlanningTest {
    @Test
    public void unsetDaysRepeatTheDefaultForecastAndSavedDaysWin() {
        List<String> saved = List.of("Storm", "Clear");
        assertEquals(WeatherPlanning.Condition.STORM, WeatherPlanning.conditionAt(saved, 1));
        assertEquals(WeatherPlanning.Condition.CLEAR, WeatherPlanning.conditionAt(saved, 2));
        assertEquals(WeatherPlanning.Condition.CLEAR, WeatherPlanning.conditionAt(saved, 3));
        assertEquals(WeatherPlanning.Condition.RAIN, WeatherPlanning.conditionAt(saved, 5));
        assertEquals(WeatherPlanning.Condition.RAIN, WeatherPlanning.conditionAt(saved, 8));
    }

    @Test
    public void editingOneForecastFillsEarlierDefaultsAndSurvivesReload() {
        InMemoryKeyValueStorage storage = new InMemoryKeyValueStorage();
        PrototypeStore store = PrototypeStore.forStorage(storage);
        store.setForecastCondition("trip", 4, WeatherPlanning.Condition.STORM);
        store.setForecastCondition("trip", 2, WeatherPlanning.Condition.CLEAR);

        List<String> forecast = PrototypeStore.forStorage(storage).weather("trip");
        assertEquals(List.of("Clear", "Clear", "Clear", "Storm"), forecast);
        assertEquals(WeatherPlanning.Condition.RAIN, WeatherPlanning.conditionAt(forecast, 5));
    }

    @Test
    public void outdoorEditPreservesCostCurrencyAndCategory() {
        ItemDetails saved = new ItemDetails(450, "PHP", "Food", true);
        ItemDetails edited = saved.withOutdoor(false);
        assertFalse(edited.isOutdoor());
        assertEquals(450, edited.getCost(), 0.0);
        assertEquals("PHP", edited.getCostCurrency());
        assertEquals("Food", edited.getCategory());
    }

    @Test
    public void bookedOutdoorStopIsFlaggedUntilUserMarksItIndoor() {
        PrototypeStore store = PrototypeStore.forStorage(new InMemoryKeyValueStorage());
        ItineraryItemWithPlace booked = new ItineraryItemWithPlace();
        booked.item = new ItineraryItemEntity("stop", "trip", null, "Meiji Shrine", 2,
                "16:00", 60, true, null, null, 0);
        List<ItineraryItemWithPlace> itinerary = List.of(booked);

        assertEquals(itinerary, WeatherPlanning.conflicts(
                itinerary, 2, WeatherPlanning.Condition.STORM, store));
        assertEquals(List.of(), WeatherPlanning.conflicts(
                itinerary, 2, WeatherPlanning.Condition.CLEAR, store));
        store.setItemDetails("stop", new ItemDetails(0, "JPY", "Activities", false));
        assertEquals(List.of(), WeatherPlanning.conflicts(
                itinerary, 2, WeatherPlanning.Condition.STORM, store));
    }

}
