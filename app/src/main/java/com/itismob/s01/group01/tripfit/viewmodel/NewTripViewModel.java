package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.prototype.TripFormRules;

public class NewTripViewModel extends ViewModel {

    private final TripRepository repository;
    private final MutableLiveData<Event<Result<String>>> createResult = new MutableLiveData<>();

    private final MutableLiveData<Event<TripUpdateOutcome>> updateResult = new MutableLiveData<>();

    private boolean pending;

    public NewTripViewModel(TripRepository repository) {
        this.repository = repository;
    }

    public LiveData<Event<TripUpdateOutcome>> getUpdateResult() {
        return updateResult;
    }

    /**
     * Saves edited settings: refuses to shorten below the last day that has a stop, otherwise
     * renames with an UPDATE (never an insert, which would REPLACE the trip and delete its stops).
     */
    public void updateTrip(String tripId, String name, int newDays) {
        pending = true;
        repository.maxStopDay(tripId, maxDay -> {
            if (!maxDay.isSuccess()) {
                deliverUpdate(TripUpdateOutcome.failed());
            } else if (!TripFormRules.canShorten(newDays, maxDay.getData())) {
                deliverUpdate(TripUpdateOutcome.blocked(maxDay.getData()));
            } else {
                repository.renameTrip(tripId, name, renamed -> deliverUpdate(
                        renamed.isSuccess() ? TripUpdateOutcome.saved() : TripUpdateOutcome.failed()));
            }
        });
    }

    /** True from the moment a save starts until its result is delivered; survives rotation. */
    public boolean isPending() {
        return pending;
    }

    private void deliverUpdate(TripUpdateOutcome outcome) {
        pending = false;
        updateResult.setValue(new Event<>(outcome));
    }

    public LiveData<Event<Result<String>>> getCreateResult() {
        return createResult;
    }

    public void createTrip(String name) {
        pending = true;
        repository.createTrip(name, result -> {
            pending = false;
            createResult.setValue(new Event<>(result));
        });
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
