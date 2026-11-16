package com.itismob.s01.group01.tripfit.prototype;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s01.group01.tripfit.HomeActivity;
import java.util.ArrayList;
import java.util.List;

public class PlannerActivity extends PrototypeActivity {
    private int selectedDay=1;
    @Override protected void onCreate(Bundle state) { super.onCreate(state); if(state!=null) selectedDay=state.getInt("day",1); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("day",selectedDay); super.onSaveInstanceState(state); }
    @Override protected void onResume() { super.onResume(); reload(); if(trip==null) { finish(); return; } render(); }
    private void render() {
        selectedDay=Math.min(selectedDay,trip.days);
        page(trip.name,trip.city+" · "+trip.days+" days · "+trip.stops.size()+" stops",true);
        notice(data.routeSource+" Booked times stay fixed. Calendar sync is a separate preview.");
        button(content,"Refresh travel information",()->refreshTravelData(this::render));
        LinearLayout overview=card(content);
        overview.addView(text(money(trip.totalBudget(),trip.homeCurrency),24,NAVY));
        label(overview,"Trip budget · "+trip.startDate+" onward");
        button(overview,"Trip settings",()->open(TripSetupActivity.class));
        button(content,"Discover places & ready-made plans",()->openFeature("DiscoverActivity"));
        ChipGroup tools=new ChipGroup(this);
        String[] labels={"Budget","Weather","Calendar & export","Group","Journal"};
        String[] screens={"BudgetActivity","WeatherActivity","SummaryActivity","GroupActivity","JournalActivity"};
        for(int i=0;i<labels.length;i++) { final String screen=screens[i]; Chip chip=new Chip(this); chip.setText(labels[i]); chip.setOnClickListener(v->openFeature(screen)); tools.addView(chip); }
        content.addView(tools);
        ChipGroup tabs=new ChipGroup(this); tabs.setSingleSelection(true); tabs.setSelectionRequired(true);
        for(int day=1;day<=trip.days;day++) { final int pick=day; Chip chip=new Chip(this); chip.setText("Day "+day); chip.setCheckable(true); chip.setChecked(day==selectedDay); chip.setOnClickListener(v->{selectedDay=pick;render();}); tabs.addView(chip); }
        content.addView(tabs);
        title("Day "+selectedDay+" · "+date(selectedDay));
        List<RoutePreview.Slot> slots=RoutePreview.day(trip,selectedDay,false,data.travelTimes);
        if(slots.isEmpty()) notice("A day full of possibilities. Add your first stop or discover a suggested plan.");
        DraftTrip.Stop previous=null;
        int travelTotal=0, stayTotal=0;
        for(RoutePreview.Slot slot:slots) {
            DraftTrip.Stop stop=slot.stop;
            if(previous!=null) {
                label(content,"↓ "+data.travelTimes.mode(previous,stop)+" · ~"+slot.travel+" min travel + "+stop.buffer+" min buffer");
            }
            LinearLayout card=card(content);
            label(card,RoutePreview.time(slot.start)+" – "+RoutePreview.time(slot.end)+(stop.booked?"   • BOOKED / FIXED":"   • FLEXIBLE"));
            card.addView(text(stop.name,19,INK));
            label(card,stop.category+" · "+stop.duration+" min · "+(stop.costKnown?money(trip.convertedCost(stop,data.rates),trip.homeCurrency)+" estimate":"Price unavailable"));
            if(stop.hasLocation) label(card,String.format(java.util.Locale.US,"%.5f, %.5f",stop.lat,stop.lng));
            else label(card,"No map pin yet · travel uses a 20 min placeholder");
            if(slot.conflict) { TextView warning=text("Schedule conflict: check travel time, booked blocks or the 20:00 day limit.",14,0xffa33232); card.addView(warning); }
            button(card,"Edit stop / transport",()->edit(stop.id));
            previous=stop; travelTotal+=slot.travel; stayTotal+=stop.duration;
        }
        label(content,"Day total · "+stayTotal+" min at stops · ~"+travelTotal+" min travel, plus buffers");
        button(content,"+ Add destination or map pin",()->edit(null));
        button(content,"Preview a shorter route",()->{
            List<RoutePreview.Slot> optimized=RoutePreview.day(trip,selectedDay,true,data.travelTimes);
            StringBuilder description=new StringBuilder("Estimated route order; booked times remain fixed.\n\n");
            int before=0,after=0; for(RoutePreview.Slot slot:slots) before+=slot.travel;
            for(RoutePreview.Slot slot:optimized) after+=slot.travel;
            description.append("Incoming travel: ~").append(before).append(" min current / ~").append(after).append(" min preview.\n\n");
            for(RoutePreview.Slot slot:optimized) description.append(RoutePreview.time(slot.start)).append("  ").append(slot.stop.name).append(slot.conflict?" ⚠":"").append("\n");
            new MaterialAlertDialogBuilder(this).setTitle("Route preview").setMessage(description.toString())
                .setNegativeButton("Keep current",null).setPositiveButton("Apply order",(d,w)->{
                    List<DraftTrip.Stop> others=new ArrayList<>(); for(DraftTrip.Stop stop:trip.stops) if(stop.day!=selectedDay) others.add(stop);
                    for(RoutePreview.Slot slot:optimized) others.add(slot.stop); trip.stops=others; save(); render();
                }).show();
        });
        button(content,"Spread flexible stops across days",()->new MaterialAlertDialogBuilder(this)
            .setTitle("Balance the itinerary?").setMessage("Flexible stops are distributed by visit duration across your trip days. Booked activities stay on their original day and time. Review travel and any conflict warnings afterward.")
            .setNegativeButton("Cancel",null).setPositiveButton("Balance days",(d,w)->{
                DraftRules.balanceDays(trip);
                save(); render();
            }).show());
        title("Your trip toolkit");
        button(content,"Budget & expenses",()->openFeature("BudgetActivity"));
        button(content,"Weather & indoor alternatives",()->openFeature("WeatherActivity"));
        button(content,"Calendar preview & trip export",()->openFeature("SummaryActivity"));
        button(content,"Group plans & cost split",()->openFeature("GroupActivity"));
        button(content,"Photos & travel journal",()->openFeature("JournalActivity"));
    }
    private void edit(String stopId) {
        Intent intent=new Intent(this,StopEditorActivity.class); intent.putExtra(HomeActivity.EXTRA_TRIP_ID,trip.id);
        intent.putExtra("day",selectedDay); if(stopId!=null) intent.putExtra("stop_id",stopId); startActivity(intent);
    }
}
