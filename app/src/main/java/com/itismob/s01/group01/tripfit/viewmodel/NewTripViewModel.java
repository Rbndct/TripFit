package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;

public class NewTripViewModel extends ViewModel {

    private final TripRepository repository;
    private final MutableLiveData<Result<String>> createResult = new MutableLiveData<>();

    public NewTripViewModel(TripRepository repository) {
        this.repository = repository;
    }

    public LiveData<Result<String>> getCreateResult() {
        return createResult;
    }

    public void createTrip(String name) {
        repository.createTrip(name, createResult::setValue);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final TripRepository repository;

        public Factory(TripRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new NewTripViewModel(repository);
        }
    }
}
