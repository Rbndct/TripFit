package com.itismob.s01.group01.tripfit;

import android.app.Application;

import com.itismob.s01.group01.tripfit.data.AppExecutors;
import com.itismob.s01.group01.tripfit.data.RemoteDataSource;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.data.TripRepositoryImpl;
import com.itismob.s01.group01.tripfit.data.local.TripFitDatabase;

/** Manual service locator: no DI framework is warranted at this app's size (YAGNI). */
public class TripFitApp extends Application {

    private TripRepository tripRepository;
    private com.itismob.s01.group01.tripfit.data.frontend.FrontendServices frontendServices;

    @Override
    public void onCreate() {
        super.onCreate();
        TripFitDatabase database = TripFitDatabase.getInstance(this);
        tripRepository = new TripRepositoryImpl(database, AppExecutors.getInstance(), new RemoteDataSource());
        frontendServices = new com.itismob.s01.group01.tripfit.data.frontend.FrontendServices(
                new com.itismob.s01.group01.tripfit.prototype.DraftStore(this),
                new com.itismob.s01.group01.tripfit.data.frontend.SampleTravelDataSource());
    }

    public TripRepository getTripRepository() {
        return tripRepository;
    }

    public com.itismob.s01.group01.tripfit.data.frontend.FrontendServices getFrontendServices() {
        return frontendServices;
    }

    /** In-process test injection only; this is not exposed through an Android component or setting. */
    @androidx.annotation.VisibleForTesting
    public com.itismob.s01.group01.tripfit.data.frontend.FrontendServices replaceFrontendServicesForTesting(
            com.itismob.s01.group01.tripfit.data.frontend.FrontendServices replacement) {
        if(replacement==null) throw new IllegalArgumentException("Frontend services are required");
        com.itismob.s01.group01.tripfit.data.frontend.FrontendServices previous=frontendServices;
        frontendServices=replacement;
        return previous;
    }
}
