package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.PlaceSuggestion;

import android.os.Bundle;
import android.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class WeatherActivity extends PrototypeActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state); if(trip==null) { finish(); return; } render(); }
    private void render() {
        page("A plan for every forecast",trip.city+" · Weather-aware trip preview",true);
        notice("Forecast sources are shown per day. Sample conditions can be changed to test indoor alternatives. Booked activities are never moved automatically.");
        button(content,"Refresh travel information",()->refreshTravelData(this::render));
        for(int day=1;day<=trip.days;day++) {
            final int current=day;
            LinearLayout card=card(content);
            card.addView(text("Day "+day+" · "+date(day),20,INK));
            com.itismob.s01.group01.tripfit.data.frontend.DailyForecast forecastData=data.forecast(day);
            String condition=forecastData.condition;
            label(card,forecastData.display());
            if(forecastData.sample) {
                Spinner forecast=choices(card,"Sample condition for Day "+day,new String[]{"Clear","Rain","Storm"},condition);
                button(card,"Apply sample forecast for Day "+day,()->{
                    if(trip.weather==null) trip.weather=new ArrayList<>();
                    while(trip.weather.size()<trip.days) trip.weather.add("Clear");
                    trip.weather.set(current-1,forecast.getSelectedItem().toString());save();data=services.travel.cached(com.itismob.s01.group01.tripfit.data.frontend.TravelQuery.from(trip));render();
                });
            }
            int outdoor=0;
            for(DraftTrip.Stop stop:trip.stops) if(stop.day==day && stop.outdoor) {
                outdoor++;
                if(forecastData.risky()) {
                    label(card,"⚠ "+stop.name+(stop.booked?" · Booked: review with your provider":" · Consider an indoor swap"));
                    if(!stop.booked) button(card,"Indoor alternatives for "+stop.name,()->alternatives(stop));
                }
            }
            if(condition.equals("Clear")) label(card,"No weather conflicts in this forecast. Review actual weather before your trip.");
            else if(outdoor==0) label(card,"No outdoor stops to flag on this day.");
        }
    }
    private void alternatives(DraftTrip.Stop outdoor) {
        List<DraftTrip.Stop> indoor=new ArrayList<>(); List<String> labels=new ArrayList<>();
        for(DraftTrip.Stop stop:trip.stops) if(!stop.outdoor && !stop.booked && stop.day!=outdoor.day && data.forecast(stop.day).condition.equals("Clear")) {
            indoor.add(stop); labels.add("Swap days with "+stop.name+" (Day "+stop.day+")");
        }
        List<PlaceSuggestion> extras=new ArrayList<>();
        for(PlaceSuggestion place:data.places(trip.city)) if(!place.outdoor && place.category.equals("Activities")) {
            boolean exists=false; for(DraftTrip.Stop stop:trip.stops) if(place.matches(stop)) exists=true;
            if(!exists) { extras.add(place); labels.add("Replace with "+place.name+" · sample suggestion"); }
        }
        if(labels.isEmpty()) { toast("No flexible indoor alternatives. Add one through Discover or edit the stop manually."); return; }
        new MaterialAlertDialogBuilder(this).setTitle("Keep the day going indoors").setItems(labels.toArray(new String[0]),(d,index)->{
            if(index<indoor.size()) { DraftTrip.Stop selected=indoor.get(index); int oldDay=outdoor.day; outdoor.day=selected.day; selected.day=oldDay; }
            else { DraftTrip.Stop replacement=extras.get(index-indoor.size()).stop(outdoor.day); replacement.id=outdoor.id; replacement.buffer=outdoor.buffer; replacement.mode=outdoor.mode; trip.stops.set(trip.stops.indexOf(outdoor),replacement); }
            save(); toast("Itinerary updated. Review the new schedule."); render();
        }).setNegativeButton("Keep current plan",null).show();
    }
}
