package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import java.util.*;

/**
 * Immutable read request for external travel adapters. Deliberately excludes journal text,
 * photos, expense amounts, members and votes. Map adapters to their required fields only.
 */
public final class TravelQuery {
    public final String destinationId, city, startDate, homeCurrency, timeZone;
    public final int days;
    public final List<String> currencies, sampleWeather;
    public final List<RoutePoint> stops;
    private TravelQuery(DraftTrip trip) {
        destinationId=trip==null?"":trip.destinationId; city=trip==null?"":trip.city;
        startDate=trip==null?"":trip.startDate; homeCurrency=trip==null?"PHP":trip.homeCurrency;
        timeZone=trip==null?"UTC":trip.timeZone(); days=trip==null?0:trip.days;
        Set<String> requested=new TreeSet<>(); requested.add(homeCurrency);
        List<RoutePoint> points=new ArrayList<>(); List<String> weather=new ArrayList<>();
        if(trip!=null) {
            requested.add(trip.localCurrency());
            for(DraftTrip.Stop stop:trip.stops) { points.add(new RoutePoint(stop)); requested.add(trip.costCurrency(stop)); }
            for(DraftTrip.Expense expense:trip.expenses) requested.add(expense.currency);
            if(trip.weather!=null) weather.addAll(trip.weather);
        }
        currencies=Collections.unmodifiableList(new ArrayList<>(requested));
        stops=Collections.unmodifiableList(points); sampleWeather=Collections.unmodifiableList(weather);
    }
    public static TravelQuery from(DraftTrip trip) { return new TravelQuery(trip); }
    @Override public boolean equals(Object other) {
        if(!(other instanceof TravelQuery)) return false; TravelQuery that=(TravelQuery)other;
        return days==that.days && Objects.equals(destinationId,that.destinationId) && city.equals(that.city)
            && startDate.equals(that.startDate) && homeCurrency.equals(that.homeCurrency) && timeZone.equals(that.timeZone)
            && currencies.equals(that.currencies) && sampleWeather.equals(that.sampleWeather) && stops.equals(that.stops);
    }
    @Override public int hashCode() { return Objects.hash(destinationId,city,startDate,homeCurrency,timeZone,days,currencies,sampleWeather,stops); }
    public static final class RoutePoint {
        public final String id, mode;
        public final double lat,lng;
        public final boolean hasLocation,booked;
        public final int day,duration,buffer,fixedMinute;
        private RoutePoint(DraftTrip.Stop stop) {
            id=stop.id; mode=stop.mode; lat=stop.lat; lng=stop.lng; hasLocation=stop.hasLocation;
            booked=stop.booked; day=stop.day; duration=stop.duration; buffer=stop.buffer; fixedMinute=stop.fixedMinute;
        }
        @Override public boolean equals(Object other) {
            if(!(other instanceof RoutePoint)) return false; RoutePoint p=(RoutePoint)other;
            return id.equals(p.id) && mode.equals(p.mode) && lat==p.lat && lng==p.lng && hasLocation==p.hasLocation
                && booked==p.booked && day==p.day && duration==p.duration && buffer==p.buffer && fixedMinute==p.fixedMinute;
        }
        @Override public int hashCode() { return Objects.hash(id,mode,lat,lng,hasLocation,booked,day,duration,buffer,fixedMinute); }
    }
}
