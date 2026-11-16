package com.itismob.s01.group01.tripfit.prototype;

import android.os.Bundle;
import android.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class WeatherActivity extends PrototypeActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state); if(trip==null) { finish(); return; } render(); }
    private void render() {
        page("A plan for every forecast",trip.city+" · Weather-aware trip preview",true);
        notice("Sample forecast · This is not live weather. Choose a condition to test outdoor warnings and indoor alternatives. Booked activities are never moved automatically.");
        if(trip.weather==null) trip.weather=new ArrayList<>();
        while(trip.weather.size()<trip.days) trip.weather.add(trip.weather.size()%3==1?"Rain":"Clear");
        for(int day=1;day<=trip.days;day++) {
            final int current=day;
            LinearLayout card=card(content);
            card.addView(text("Day "+day+" · "+date(day),20,INK));
            String condition=trip.weather.get(day-1);
            label(card,(condition.equals("Rain")?"☂ Rain · 22°C · 80% sample chance":condition.equals("Storm")?"Storm · 20°C · 95% sample chance":"☀ Clear · 26°C · 10% sample chance"));
            Spinner forecast=choices(card,"Sample condition",new String[]{"Clear","Rain","Storm"},condition);
            button(card,"Apply sample forecast",()->{trip.weather.set(current-1,forecast.getSelectedItem().toString());save();render();});
            int outdoor=0;
            for(DraftTrip.Stop stop:trip.stops) if(stop.day==day && stop.outdoor) {
                outdoor++;
                if(!condition.equals("Clear")) {
                    label(card,"⚠ "+stop.name+(stop.booked?" · Booked: review with your provider":" · Consider an indoor swap"));
                    if(!stop.booked) button(card,"Indoor alternatives for "+stop.name,()->alternatives(stop));
                }
            }
            if(condition.equals("Clear")) label(card,"No sample weather conflicts. Review actual weather before your trip.");
            else if(outdoor==0) label(card,"No outdoor stops to flag on this day.");
        }
    }
    private void alternatives(DraftTrip.Stop outdoor) {
        List<DraftTrip.Stop> indoor=new ArrayList<>(); List<String> labels=new ArrayList<>();
        for(DraftTrip.Stop stop:trip.stops) if(!stop.outdoor && !stop.booked && stop.day!=outdoor.day) {
            indoor.add(stop); labels.add("Swap days with "+stop.name+" (Day "+stop.day+")");
        }
        List<DemoCatalog.Place> extras=new ArrayList<>();
        for(DemoCatalog.Place place:DemoCatalog.places(trip.city)) if(!place.outdoor && place.category.equals("Activities")) {
            boolean exists=false; for(DraftTrip.Stop stop:trip.stops) if(stop.name.equals(place.name)) exists=true;
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
