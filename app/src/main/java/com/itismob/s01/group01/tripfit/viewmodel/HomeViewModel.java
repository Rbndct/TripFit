package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;

import java.util.Collections;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final LiveData<List<TripEntity>> trips;
    private final LiveData<TripEntity> firstTrip;
    private final LiveData<List<ItineraryItemEntity>> firstTripItems;

    public HomeViewModel(TripRepository repository) {
        this.trips = repository.observeTrips();
        this.firstTrip = Transformations.map(trips,
                list -> list.isEmpty() ? null : list.get(0));
        this.firstTripItems = Transformations.switchMap(firstTrip, trip -> trip == null
                ? new MutableLiveData<>(Collections.emptyList())
                : repository.observeItineraryItems(trip.getId()));
        repository.ensureSeedData(result -> { /* trips LiveData updates on its own */ });
    }

    public LiveData<TripEntity> getFirstTrip() {
        return firstTrip;
    }

    public LiveData<List<ItineraryItemEntity>> getFirstTripItems() {
        return firstTripItems;
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final TripRepository repository;

        public Factory(TripRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new HomeViewModel(repository);
        }
    }
}
