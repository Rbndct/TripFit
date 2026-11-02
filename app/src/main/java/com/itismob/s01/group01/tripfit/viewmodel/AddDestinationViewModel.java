package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;

public class AddDestinationViewModel extends ViewModel {

    private final TripRepository repository;
    private final MutableLiveData<Result<Void>> saveResult = new MutableLiveData<>();

    public AddDestinationViewModel(TripRepository repository) {
        this.repository = repository;
    }

    public LiveData<Result<Void>> getSaveResult() {
        return saveResult;
    }

    public void save(String tripId, String name, int day, Integer durationMin, boolean booked,
                      String time) {
        repository.addItineraryItem(tripId, name, day, time, durationMin, booked,
                saveResult::setValue);
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
