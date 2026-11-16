package com.itismob.s01.group01.tripfit.prototype;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.itismob.s01.group01.tripfit.HomeActivity;
import com.itismob.s01.group01.tripfit.LoginActivity;

public class TripsActivity extends PrototypeActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state); }
    @Override protected void onResume() { super.onResume(); render(); }
    private void render() {
        page("Trippings","Less planning. More exploring.",false);
        notice("Frontend preview · Your drafts stay on this device. Discovery, prices, exchange rates and weather use sample data.");
        title("Where to next?");
        label(content,"Build a trip around the places, people and budget that matter to you.");
        button(content,"+ Plan a new trip",()->open(TripSetupActivity.class));
        title("Your trips");
        for(DraftTrip draft:store.all()) {
            LinearLayout card=card(content);
            card.addView(text(draft.name,20,INK));
            label(card,draft.city+" · "+draft.days+" days · "+draft.stops.size()+" stops"+(draft.sample?" · Sample trip":""));
            label(card,draft.startDate+"  /  "+money(draft.totalBudget(),draft.homeCurrency)+" budget");
            button(card,"Open itinerary →",()->{
                Intent intent=new Intent(this,PlannerActivity.class); intent.putExtra(HomeActivity.EXTRA_TRIP_ID,draft.id); startActivity(intent);
            });
        }
        title("Made for your kind of trip");
        label(content,"Discover famous spots and hidden gems. Keep reservations fixed, choose transport per leg and track the whole trip budget.");
        button(content,"Traveler preferences",()->{
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Your prototype profile")
                .setMessage("Traveler\nLocal prototype · No account required\n\nHome currency and trip preferences can be changed in each trip. No information is synced online.")
                .setPositiveButton("Done",null).setNegativeButton("Back to welcome",(d,w)->{
                    Intent intent=new Intent(this,LoginActivity.class); intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); startActivity(intent); finish();
                }).show();
        });
    }
}
