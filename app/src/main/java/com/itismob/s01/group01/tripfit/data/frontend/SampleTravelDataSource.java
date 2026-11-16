package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import java.util.*;

/** Current local adapter. No HTTP calls, tokens, accounts, or implicit uploads. */
public final class SampleTravelDataSource implements TravelDataSource {
    @Override public TravelSnapshot cached(TravelQuery trip) {
        List<Destination> cities=Arrays.asList(
            new Destination("sample:tokyo","Tokyo","JPY","Asia/Tokyo",35.6762,139.6503),
            new Destination("sample:manila","Manila","PHP","Asia/Manila",14.5995,120.9842));
        List<PlaceSuggestion> places=new ArrayList<>(); for(Destination city:cities) places.addAll(DemoCatalog.places(city.name));
        Map<Integer,DailyForecast> forecasts=new HashMap<>();
        if(trip!=null) for(int day=1;day<=trip.days;day++) {
            String condition=trip.sampleWeather.size()>=day?trip.sampleWeather.get(day-1):day%3==2?"Rain":"Clear";
            forecasts.put(day,new DailyForecast(condition,condition.equals("Rain")?22:condition.equals("Storm")?20:26,
                    condition.equals("Rain")?80:condition.equals("Storm")?95:10,"Sample forecast",true));
        }
        return new TravelSnapshot(cities,places,ExchangeRates.samples(),new SampleTravelTimes(),forecasts,
            "Sample recommendations · Prices, restaurants, ratings and review counts are illustrative.",
            "Route preview · Straight-line travel estimates, not live street or transit routing.");
    }
    @Override public void refresh(TravelQuery trip,Result.Callback<TravelSnapshot> callback) { callback.onResult(Result.success(cached(trip))); }
}
