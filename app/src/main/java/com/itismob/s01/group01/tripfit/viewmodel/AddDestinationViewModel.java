package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.ItemDetails;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AddDestinationViewModel extends ViewModel {

    private final TripRepository repository;
    private final MutableLiveData<Result<Void>> saveResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<CityDto>>> citiesResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<PlaceDto>>> searchResult = new MutableLiveData<>();
    private boolean saving;
    private int searchVersion;

    public AddDestinationViewModel(TripRepository repository) {
        this.repository = repository;
    }

    public LiveData<Result<Void>> getSaveResult() {
        return saveResult;
    }

    public LiveData<Result<List<CityDto>>> getCitiesResult() {
        return citiesResult;
    }

    public LiveData<Result<List<PlaceDto>>> getSearchResult() {
        return searchResult;
    }

    public void loadCities() {
        repository.fetchCities(citiesResult::setValue);
    }

    public void invalidateSearch() {
        searchVersion++;
        searchResult.setValue(null);
    }

    /** A blank query lists the bundled city catalog without a remote call. */
    public void search(String cityId, String query) {
        int version = ++searchVersion;
        if (cityId == null) {
            searchResult.setValue(Result.success(Collections.emptyList()));
            return;
        }
        repository.searchPlaces(cityId, query.trim(), result -> {
            if (version == searchVersion) {
                searchResult.setValue(result);
            }
        });
    }

    public boolean isSaving() {
        return saving;
    }

    /** Saves a free-text stop with no database place match. */
    public void save(String tripId, String name, int day, Integer durationMin, boolean booked,
                      String time) {
        save(UUID.randomUUID().toString(), tripId, name, day, durationMin, booked, time);
    }

    /** Saves a free-text stop under a caller-chosen id (so its cost can be keyed to it). */
    public void save(String itemId, String tripId, String name, int day, Integer durationMin,
                     boolean booked, String time) {
        if (saving) {
            return;
        }
        saving = true;
        repository.addItineraryItem(itemId, tripId, name, day, time, durationMin, booked,
                this::finishSave);
    }

    private void finishSave(Result<Void> result) {
        saving = false; // cleared first so a failed save can be retried
        saveResult.setValue(result);
    }

    /** Saves a stop backed by a place the user searched for and selected. */
    public void saveFromPlace(String itemId, String tripId, PlaceDto place, int day,
                              Integer durationMin, boolean booked, String time) {
        if (saving) {
            return;
        }
        saving = true;
        repository.addItineraryItemFromPlace(itemId, tripId, place, day, time, durationMin, booked,
                this::finishSave);
    }

    public void saveFromCatalog(String itemId, String tripId, DemoCatalog.Place place, int day,
                                int durationMin, boolean booked, String time, ItemDetails details) {
        if (saving) {
            return;
        }
        saving = true;
        repository.addCatalogPlace(itemId, tripId, place, day, time, durationMin, booked, details,
                this::finishSave);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final TripRepository repository;

        public Factory(TripRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AddDestinationViewModel(repository);
        }
    }
}
