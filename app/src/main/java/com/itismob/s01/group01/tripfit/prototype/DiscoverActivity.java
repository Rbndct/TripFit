package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.PlaceSuggestion;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiscoverActivity extends PrototypeActivity {
    private String filter="All places", query="";
    private boolean withinBudget;
    private int day=1;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if(trip==null) { finish(); return; }
        if(state!=null) { filter=state.getString("filter","All places"); query=state.getString("query",""); day=state.getInt("day",1); withinBudget=state.getBoolean("budget"); }
        render();
    }
    private void render() {
        page("Discover "+trip.city,"Must-sees, little discoveries and room for spontaneity.",true);
        notice(data.placeSource+" Distances are straight-line estimates, not walking routes.");
        button(content,"Refresh travel information",()->refreshTravelData(this::render));
        Spinner kind=choices(content,"What would you like to find?",new String[]{"All places","Famous / must-see","Hidden gems","Food","Activities"},filter);
        TextInputEditText search=field(content,"Search places",query,InputType.TYPE_CLASS_TEXT);
        String[] days=new String[trip.days]; for(int i=0;i<days.length;i++) days[i]="Day "+(i+1);
        Spinner targetDay=choices(content,"Add suggestions to",days,"Day "+day);
        CheckBox affordable=check(content,"Only show places within remaining category budget",withinBudget);
        button(content,"Update suggestions",()->{ filter=kind.getSelectedItem().toString(); query=value(search); day=targetDay.getSelectedItemPosition()+1; withinBudget=affordable.isChecked(); render(); });
        button(content,"Build a suggested itinerary",this::suggestedPlan);
        title("Suggestions for Day "+day);
        double anchorLat=data.destination(trip.city).lat, anchorLng=data.destination(trip.city).lng;
        String anchorName="city center";
        for(DraftTrip.Stop s:trip.stops) if(s.day==day && s.hasLocation) { anchorLat=s.lat; anchorLng=s.lng; anchorName=s.name; }
        label(content,"Nearby "+anchorName+" · Change the day to use that day's last pinned stop.");
        List<PlaceSuggestion> candidates=data.places(trip.city);
        final double lat=anchorLat,lng=anchorLng;
        candidates.sort(java.util.Comparator.comparingDouble(p->RoutePreview.distance(lat,lng,p.lat,p.lng)));
        int count=0;
        for(PlaceSuggestion p:candidates) {
            if(!query.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            if(filter.equals("Famous / must-see") && (p.hidden || p.category.equals("Food"))) continue;
            if(filter.equals("Hidden gems") && !p.hidden) continue;
            if(filter.equals("Food") && !p.category.equals("Food")) continue;
            if(filter.equals("Activities") && !p.category.equals("Activities")) continue;
            double converted=p.price==null?0:data.rates.convert(p.price,p.currency,trip.homeCurrency);
            double remaining=remaining(p.category);
            if(withinBudget && (p.price==null || converted>remaining)) continue;
            count++;
            LinearLayout card=card(content);
            label(card,(p.hidden?"HIDDEN GEM":"POPULAR PICK")+" · "+p.category+(p.outdoor?" · Outdoors":" · Indoors"));
            card.addView(text(p.name,20,INK)); label(card,p.description);
            label(card,String.format(Locale.US,"★ %.1f · %,d reviews · %.1f km away",p.rating,p.reviews,RoutePreview.distance(anchorLat,anchorLng,p.lat,p.lng)));
            String price=p.price==null?"Price estimate unavailable":money(p.price,p.currency)+" ≈ "+money(converted,trip.homeCurrency);
            label(card,p.tierLabel()+" · "+price+" · "+p.minutes+" min");
            label(card,money(remaining,trip.homeCurrency)+" remaining in "+p.category+(converted>remaining?" · Over category budget":""));
            boolean already=false; for(DraftTrip.Stop s:trip.stops) if(p.matches(s)) already=true;
            if(already) label(card,"Already in your itinerary");
            else button(card,"Add to Day "+day,()->{ trip.stops.add(p.stop(day)); save(); toast("Added to your itinerary"); render(); });
            button(card,"Swap with a flexible stop",()->swap(p));
        }
        if(count==0) notice("No sample places match these filters. Try all places, turn off the budget filter or add your own stop from the itinerary.");
    }
    private double remaining(String category) {
        return DraftBudget.suggestionAllowance(trip,data.rates,category,day);
    }
    private void suggestedPlan() {
        List<PlaceSuggestion> picks=new ArrayList<>();
        for(boolean hidden:new boolean[]{false,true}) {
            int limit=hidden?2:3, added=0;
            for(PlaceSuggestion place:data.places(trip.city)) {
                boolean duplicate=false; for(DraftTrip.Stop stop:trip.stops) if(place.matches(stop)) duplicate=true;
                if(!duplicate && place.hidden==hidden && !place.category.equals("Food") && added<limit) { picks.add(place); added++; }
            }
        }
        if(picks.isEmpty()) { toast("These sample attractions are already in your trip. Try adding a place manually."); return; }
        String[] names=new String[picks.size()]; boolean[] checked=new boolean[picks.size()];
        for(int i=0;i<picks.size();i++) { names[i]=(picks.get(i).hidden?"Hidden gem: ":"Must-see: ")+picks.get(i).name; checked[i]=true; }
        new MaterialAlertDialogBuilder(this).setTitle("Make this plan yours")
            .setMultiChoiceItems(names,checked,(dialog,index,selected)->checked[index]=selected)
            .setNegativeButton("Cancel",null).setPositiveButton("Add selected stops",(dialog,which)->{
                int added=0; for(int i=0;i<picks.size();i++) if(checked[i]) {
                    DraftTrip.Stop stop=picks.get(i).stop(Math.min(trip.days,day+added/3)); trip.stops.add(stop); added++;
                }
                save(); toast(added+" stops added across your trip days"); render();
            }).show();
    }
    private void swap(PlaceSuggestion place) {
        for(DraftTrip.Stop stop:trip.stops) if(place.matches(stop)) { toast("This place is already in your itinerary."); return; }
        List<DraftTrip.Stop> flexible=new ArrayList<>(); List<String> names=new ArrayList<>();
        for(DraftTrip.Stop stop:trip.stops) if(!stop.booked) { flexible.add(stop); names.add("Day "+stop.day+" · "+stop.name); }
        if(flexible.isEmpty()) { toast("No flexible stops to swap. Booked activities stay protected."); return; }
        new MaterialAlertDialogBuilder(this).setTitle("Replace a flexible stop").setItems(names.toArray(new String[0]),(dialog,index)->{
            DraftTrip.Stop old=flexible.get(index), replacement=place.stop(old.day); replacement.id=old.id; replacement.mode=old.mode; replacement.buffer=old.buffer;
            trip.stops.set(trip.stops.indexOf(old),replacement); save(); toast("Stop swapped"); render();
        }).setNegativeButton("Cancel",null).show();
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("filter",filter); state.putString("query",query); state.putInt("day",day); state.putBoolean("budget",withinBudget); super.onSaveInstanceState(state); }
}
