package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import java.util.*;

/** Cached data used by all screens and exports, including its provenance. */
public final class TravelSnapshot {
    public final List<Destination> destinations;
    public final List<PlaceSuggestion> places;
    public final ExchangeRates rates;
    public final TravelTimeProvider travelTimes;
    public final String placeSource, routeSource;
    private final Map<Integer,DailyForecast> forecasts;
    public TravelSnapshot(List<Destination> destinations,List<PlaceSuggestion> places,ExchangeRates rates,
            TravelTimeProvider travelTimes,Map<Integer,DailyForecast> forecasts,String placeSource,String routeSource) {
        this.destinations=Collections.unmodifiableList(new ArrayList<>(destinations));
        this.places=Collections.unmodifiableList(new ArrayList<>(places)); this.rates=rates;
        this.travelTimes=travelTimes; this.forecasts=Collections.unmodifiableMap(new HashMap<>(forecasts));
        this.placeSource=placeSource; this.routeSource=routeSource;
    }
    public Destination destination(String name) {
        for(Destination city:destinations) if(city.name.equals(name)) return city;
        throw new IllegalArgumentException("Destination metadata is unavailable");
    }
    public List<PlaceSuggestion> places(String city) {
        List<PlaceSuggestion> matches=new ArrayList<>(); for(PlaceSuggestion place:places) if(place.city.equals(city)) matches.add(place); return matches;
    }
    public DailyForecast forecast(int day) {
        DailyForecast forecast=forecasts.get(day);
        return forecast!=null?forecast:new DailyForecast("Unavailable",null,null,"No cached forecast",false);
    }
    public void validateFor(DraftTrip trip) {
        if(trip==null) return;
        if(!rates.supports(trip.homeCurrency)) throw new IllegalArgumentException("Missing home-currency rate");
        for(DraftTrip.Stop stop:trip.stops) if(!rates.supports(trip.costCurrency(stop))) throw new IllegalArgumentException("Missing stop-currency rate");
        for(DraftTrip.Expense expense:trip.expenses) if(!rates.supports(expense.currency)) throw new IllegalArgumentException("Missing expense-currency rate");
        for(PlaceSuggestion place:places) if(!rates.supports(place.currency)) throw new IllegalArgumentException("Missing recommendation-currency rate");
    }
}
