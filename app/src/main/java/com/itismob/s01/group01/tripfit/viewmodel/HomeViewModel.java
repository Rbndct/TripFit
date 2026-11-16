package com.itismob.s01.group01.tripfit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.local.TripItemCount;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.ui.HomeTripRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {

    private final MediatorLiveData<List<HomeTripRow>> tripRows = new MediatorLiveData<>();

    private List<TripEntity> latestTrips = new ArrayList<>();
    private Map<String, TripItemCount> latestCounts = new HashMap<>();

    public HomeViewModel(TripRepository repository) {
        LiveData<List<TripEntity>> trips = repository.observeTrips();
        LiveData<List<TripItemCount>> counts = repository.observeTripItemCounts();

        tripRows.addSource(trips, value -> {
            latestTrips = value != null ? value : new ArrayList<>();
            tripRows.setValue(buildRows());
        });
        tripRows.addSource(counts, value -> {
            latestCounts = indexByTripId(value);
            tripRows.setValue(buildRows());
        });

        repository.ensureSeedData(result -> { /* trips LiveData updates on its own */ });
    }

    public LiveData<List<HomeTripRow>> getTripRows() {
        return tripRows;
    }

    private List<HomeTripRow> buildRows() {
        List<HomeTripRow> rows = new ArrayList<>();
        for (TripEntity trip : latestTrips) {
            TripItemCount count = latestCounts.get(trip.getId());
            int stopCount = count != null ? count.stopCount : 0;
            rows.add(buildRow(trip, stopCount));
        }
        return rows;
    }

    /** Trip length, city, start date and budget come from the persistent store's trip details. */
    private HomeTripRow buildRow(TripEntity trip, int stopCount) {
        PrototypeStore store = PrototypeStore.get();
        TripMeta meta = store.meta(trip.getId());
        BudgetSettings budget = store.budget(trip.getId());
        String total = CurrencyRates.format(budget.tripTotal(meta.getDays()), budget.getHomeCurrency());
        return new HomeTripRow(trip.getId(), trip.getName(), meta.getDays(), stopCount,
                meta.getCity(), meta.getStartDateIso(), store.isSample(trip.getId()), total);
    }

    /** Rebuilds the rows after trip details or budgets changed in the store (not in Room). */
    public void refresh() {
        tripRows.setValue(buildRows());
    }

    private static Map<String, TripItemCount> indexByTripId(List<TripItemCount> counts) {
        Map<String, TripItemCount> byTripId = new HashMap<>();
        if (counts != null) {
            for (TripItemCount count : counts) {
                byTripId.put(count.tripId, count);
            }
        }
        return byTripId;
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
