package com.itismob.s01.group01.tripfit;

import android.app.Application;

import com.itismob.s01.group01.tripfit.data.AppExecutors;
import com.itismob.s01.group01.tripfit.data.CurrencyRepository;
import com.itismob.s01.group01.tripfit.data.RemoteDataSource;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;
import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;

/** Manual service locator: no DI framework is warranted at this app's size (YAGNI). */
public class TripFitApp extends Application {

    private TripRepository tripRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        TripFitDatabase database = TripFitDatabase.getInstance(this);
        tripRepository = new TripRepositoryImpl(database, AppExecutors.getInstance(), new RemoteDataSource());
        startCurrencyRates();
    }

    /**
     * Use the last good exchange rates straight away (so an offline start still has the full
     * currency list), then refresh in the background when they are missing or over 12 hours old.
     */
    private void startCurrencyRates() {
        CurrencyRepository currencyRepository = CurrencyRepository.create(this);
        currencyRepository.loadCached();
        currencyRepository.refreshIfStale();
    }

    public TripRepository getTripRepository() {
        return tripRepository;
    }
}
