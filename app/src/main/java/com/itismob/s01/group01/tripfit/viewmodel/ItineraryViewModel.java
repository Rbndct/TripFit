package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;

import java.util.List;

public class ItineraryViewModel extends ViewModel {

    private final LiveData<List<ItineraryItemWithPlace>> items;

    public ItineraryViewModel(TripRepository repository, String tripId) {
        this.items = repository.observeItineraryItemsWithPlaceNames(tripId);
    }

    public LiveData<List<ItineraryItemWithPlace>> getItems() {
        return items;
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final TripRepository repository;
        private final String tripId;

        public Factory(TripRepository repository, String tripId) {
            this.repository = repository;
            this.tripId = tripId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ItineraryViewModel(repository, tripId);
        }
    }
}
