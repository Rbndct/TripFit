package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.PlaceSuggestion;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/** Stores frontend drafts only. Existing Room data is left intact. */
public final class DraftStore implements com.itismob.s01.group01.tripfit.data.frontend.DraftRepository {
    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    public DraftStore(Context context) { preferences=context.getSharedPreferences("trippings_prototype_v1",Context.MODE_PRIVATE); }
    public List<DraftTrip> all() {
        String json=preferences.getString("trips",null);
        if(json!=null) return gson.fromJson(json,new TypeToken<List<DraftTrip>>(){}.getType());
        DraftTrip sample=new DraftTrip();
        sample.name="Tokyo, at your own pace"; sample.sample=true;
        sample.members.add("You"); sample.members.add("Alex (sample)");
        for(PlaceSuggestion place:DemoCatalog.places("Tokyo")) {
            if(sample.stops.size()>=6) break;
            DraftTrip.Stop stop=place.stop(sample.stops.size()<3?1:2);
            if(place.name.equals("Tokyo Skytree")) { stop.booked=true; stop.fixedMinute=1080; }
            if(place.name.equals("teamLab Planets")) { stop.booked=true; stop.fixedMinute=930; }
            sample.stops.add(stop);
        }
        List<DraftTrip> trips=new ArrayList<>(); trips.add(sample); write(trips); return trips;
    }
    public DraftTrip get(String id) { for(DraftTrip trip:all()) if(trip.id.equals(id)) return trip; return null; }
    public void save(DraftTrip trip) {
        List<DraftTrip> trips=all();
        for(int i=0;i<trips.size();i++) if(trips.get(i).id.equals(trip.id)) { trips.set(i,trip); write(trips); return; }
        trips.add(0,trip); write(trips);
    }
    private void write(List<DraftTrip> trips) { preferences.edit().putString("trips",gson.toJson(trips)).apply(); }
    @Override public void refresh(com.itismob.s01.group01.tripfit.data.Result.Callback<List<DraftTrip>> callback) {
        callback.onResult(com.itismob.s01.group01.tripfit.data.Result.success(all()));
    }
}
