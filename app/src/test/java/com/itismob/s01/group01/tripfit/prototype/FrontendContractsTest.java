package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class FrontendContractsTest {
    @Test public void olderSavedDraftsKeepCurrencyBookingAndExpenseDefaults() {
        String legacy="{\"city\":\"Manila\",\"homeCurrency\":\"PHP\",\"stops\":[{\"id\":\"old-stop\",\"name\":\"Reservation\",\"day\":2,\"booked\":true,\"fixedMinute\":900}],\"expenses\":[{\"amount\":120,\"currency\":\"PHP\",\"category\":\"Food\"}]}";
        DraftTrip trip=new com.google.gson.Gson().fromJson(legacy,DraftTrip.class);
        assertEquals("PHP",trip.localCurrency()); assertEquals("Asia/Manila",trip.timeZone());
        assertTrue(trip.stops.get(0).costKnown); assertTrue(trip.stops.get(0).booked); assertEquals(900,trip.stops.get(0).fixedMinute);
        assertEquals(1,trip.expenses.get(0).day); assertNotNull(trip.journal);
    }
    @Test public void travelQueriesAreImmutableAndExcludePrivateTripContent() {
        DraftTrip trip=new DraftTrip(); DraftTrip.Stop stop=new DraftTrip.Stop(); stop.lat=35; trip.stops.add(stop);
        DraftTrip.Entry entry=new DraftTrip.Entry(); entry.note="private journal marker"; entry.photo="private-photo-uri"; trip.journal.add(entry);
        trip.members.add("private companion marker");
        TravelQuery request=TravelQuery.from(trip); stop.lat=40; trip.weather.add("Storm");
        assertEquals(35,request.stops.get(0).lat,.001); assertTrue(request.sampleWeather.isEmpty());
        String json=new com.google.gson.Gson().toJson(request);
        assertFalse(json.contains("private journal marker")); assertFalse(json.contains("private-photo-uri")); assertFalse(json.contains("private companion marker"));
    }
    @Test(expected=IllegalArgumentException.class) public void malformedPlaceApiResponseIsRejected() {
        new PlaceSuggestion("bad","Tokyo","Bad price","Food","JPY",35,139,-1d,4,5,60,false,false,"");
    }
    private ExchangeRates customRates() {
        Map<String,Double> rates=new LinkedHashMap<>(); rates.put("EUR",1d); rates.put("USD",2d);
        return new ExchangeRates(rates,"Injected test rates",false,123L);
    }
    @Test public void rateTablesAreImmutableAndDoNotSilentlyAssumePhp() {
        Map<String,Double> values=new HashMap<>(); values.put("USD",2d); values.put("EUR",1d);
        ExchangeRates rates=new ExchangeRates(values,"Test",false,123L); values.put("USD",100d);
        assertEquals(10,rates.convert(20,"EUR","USD"),.001);
        try { rates.convert(1,"GBP","USD"); fail("Missing rates must be reported"); } catch(IllegalArgumentException expected) {}
    }
    @Test(expected=IllegalArgumentException.class) public void zeroApiExchangeRateIsRejected() {
        Map<String,Double> values=new HashMap<>(); values.put("USD",0d); new ExchangeRates(values,"Bad response",false,1L);
    }
    @Test public void newDestinationAndApiRatesWorkWithoutChangingScreensOrExportRules() {
        DraftTrip trip=new DraftTrip(); trip.city="Paris"; trip.destinationId="city-42";
        trip.destinationCurrency="EUR"; trip.destinationTimeZone="Europe/Paris"; trip.homeCurrency="USD";
        PlaceSuggestion place=new PlaceSuggestion("place-42","Paris","Gallery","Activities","EUR",48.86,2.34,20d,4.7,42,60,true,false,"Test API place");
        trip.stops.add(place.stop(1));
        TravelSnapshot data=new TravelSnapshot(Collections.singletonList(new Destination("city-42","Paris","EUR","Europe/Paris",48.86,2.34)),
            Collections.singletonList(place),customRates(),new SampleTravelTimes(),Collections.emptyMap(),"Test API places","Test routing cache");
        data.validateFor(trip);
        assertEquals(10,trip.convertedCost(trip.stops.get(0),data.rates),.001);
        assertTrue(TripExport.calendar(trip,data).contains("DTSTART:20261201T080000Z"));
        assertTrue(TripExport.summary(trip,data).contains("Injected test rates"));
        assertEquals("Unavailable",data.forecast(1).condition);
    }
    @Test public void opaquePlaceIdsSurviveLabelChangesAndUnknownPriceIsNotFree() {
        PlaceSuggestion old=new PlaceSuggestion("vendor:123","Tokyo","Old name","Activities","JPY",35,139,null,4,2,60,false,true,"",2);
        DraftTrip.Stop stop=old.stop(1); stop.name="Custom label";
        assertTrue(old.matches(stop)); assertFalse(stop.costKnown); assertNotEquals("Free",old.tierLabel());
    }
    @Test public void scheduleUsesInjectedTravelTimesAndStillProtectsBookings() {
        DraftTrip trip=new DraftTrip(); DraftTrip.Stop flexible=new DraftTrip.Stop(), booked=new DraftTrip.Stop();
        flexible.duration=45; booked.booked=true; booked.fixedMinute=600; booked.duration=30;
        trip.stops.add(flexible); trip.stops.add(booked);
        TravelTimeProvider times=new TravelTimeProvider() {
            public int minutes(DraftTrip.Stop from,DraftTrip.Stop to) { return from==null?0:120; }
            public String mode(DraftTrip.Stop from,DraftTrip.Stop to) { return "Transit"; }
        };
        List<RoutePreview.Slot> slots=RoutePreview.day(trip,1,true,times);
        assertSame(booked,slots.get(0).stop); assertEquals(600,slots.get(0).start);
        assertEquals(120,slots.get(1).travel); assertTrue(slots.get(1).start>=765);
    }
    @Test public void optimizedPreviewNeverAppliesALongerTravelEstimate() {
        DraftTrip trip=new DraftTrip(); DraftTrip.Stop a=new DraftTrip.Stop(),b=new DraftTrip.Stop(),c=new DraftTrip.Stop();
        trip.stops.add(a); trip.stops.add(b); trip.stops.add(c);
        TravelTimeProvider times=new TravelTimeProvider() {
            public int minutes(DraftTrip.Stop from,DraftTrip.Stop to) {
                if(from==null) return 0; if(from==a && to==b) return 5; if(from==a && to==c) return 1;
                if(from==b && to==c) return 1; if(from==c && to==b) return 20; return 5;
            }
            public String mode(DraftTrip.Stop from,DraftTrip.Stop to) { return "Driving"; }
        };
        List<RoutePreview.Slot> result=RoutePreview.day(trip,1,true,times);
        assertSame(b,result.get(1).stop); assertEquals(6,result.get(1).travel+result.get(2).travel);
    }
    @Test public void incompleteRatesAreRejectedBeforeReplacingTheCachedSnapshot() {
        DraftTrip trip=new DraftTrip();
        TravelSnapshot data=new TravelSnapshot(Collections.emptyList(),Collections.emptyList(),customRates(),new SampleTravelTimes(),Collections.emptyMap(),"Test","Test");
        try { data.validateFor(trip); fail("PHP is missing"); } catch(IllegalArgumentException expected) {}
    }
    @Test public void sampleRefreshUsesTheSameContractWithoutRequiringNetwork() {
        TravelDataSource source=new SampleTravelDataSource(); final boolean[] called={false};
        source.refresh(TravelQuery.from(new DraftTrip()),result->{called[0]=true;assertTrue(result.isSuccess());assertFalse(result.getData().places("Tokyo").isEmpty());});
        assertTrue(called[0]);
    }
}
