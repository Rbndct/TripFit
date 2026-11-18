package com.itismob.s01.group01.tripfit.data;

import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;

import java.util.ArrayList;
import java.util.List;

/** Adapts sample catalog entries to the destination search DTOs. */
final class OfflineData {
    static List<CityDto> cities() {
        List<CityDto> cities = new ArrayList<>();
        for (DemoCatalog.City city : DemoCatalog.City.values()) {
            cities.add(toDto(city));
        }
        return cities;
    }

    static CityDto toDto(DemoCatalog.City city) {
        return new CityDto(city.id, city.name, city.country, city.lat, city.lng);
    }

    static List<PlaceDto> search(String city, String query) {
        List<PlaceDto> places = new ArrayList<>();
        for (DemoCatalog.Place place : DemoCatalog.search(city, query)) {
            places.add(toDto(place));
        }
        return places;
    }

    static PlaceDto toDto(DemoCatalog.Place place) {
        return new PlaceDto(place.id, place.catalogCity.id, place.name, place.category,
                place.lat, place.lng, place.address(), place.priceTier(), place.rating,
                place.hidden, place.description, null);
    }

    private OfflineData() { }
}
