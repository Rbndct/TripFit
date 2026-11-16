package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.PlaceSuggestion;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Locale;

public class GroupActivity extends PrototypeActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state); if(trip==null) { finish(); return; } render(); }
    @Override protected void onResume() { super.onResume(); if(root!=null && reloadIfChanged()) render(); }
    private void render() {
        page("Better together",trip.name+" · Group trip",true);
        notice("Local group preview · Members and votes are simulated on this device. No invitations are sent and no other device can join yet.");
        title("Travel companions");
        if(trip.members.isEmpty()) trip.members.add("You");
        for(String member:trip.members) label(content,"● "+member+(member.equals("You")?" · Organizer":""));
        TextInputEditText member=field(content,"Companion name","",InputType.TYPE_CLASS_TEXT);
        button(content,"Add companion to preview",()->{
            if(value(member).isEmpty()) { member.setError("Enter a name"); return; }
            for(String person:trip.members) if(person.equalsIgnoreCase(value(member))) { member.setError("This person is already listed"); return; }
            trip.members.add(value(member)); save(); render();
        });
        TextInputEditText code=field(content,"Sample join code","",InputType.TYPE_CLASS_TEXT);
        label(content,"Try TRIP-DEMO to preview a companion joining. This does not join a real shared trip.");
        button(content,"Preview joining",()->{
            if(!value(code).equalsIgnoreCase("TRIP-DEMO")) { code.setError("Use the sample code TRIP-DEMO"); return; }
            if(!trip.members.contains("Sam (sample)")) trip.members.add("Sam (sample)"); save(); render();
        });
        title("Vote on the next stop");
        label(content,"One local vote per place. Tap again to remove your vote. Add winning places through Discover.");
        for(PlaceSuggestion place:data.places(trip.city)) if(place.category.equals("Activities")) {
            LinearLayout card=card(content); card.addView(text(place.name,18,INK));
            boolean voted=trip.votes.contains(place.id) || trip.votes.contains(place.name); label(card,voted?"1 vote · You":"0 votes");
            button(card,(voted?"Remove vote for ":"Vote for ")+place.name,()->{
                if(trip.votes.contains(place.id) || trip.votes.contains(place.name)) { trip.votes.remove(place.id); trip.votes.remove(place.name); }
                else trip.votes.add(place.id); save(); render();
            });
        }
        title("Split the estimated cost");
        double estimated=0, spent=0;
        for(DraftTrip.Stop stop:trip.stops) estimated+=trip.convertedCost(stop,data.rates);
        for(DraftTrip.Expense expense:trip.expenses) spent+=data.rates.convert(expense.amount,expense.currency,trip.homeCurrency);
        LinearLayout card=card(content);
        label(card,"Planned total: "+money(estimated,trip.homeCurrency)+" · "+trip.members.size()+" travelers");
        card.addView(text(money(estimated/trip.members.size(),trip.homeCurrency)+" each",24,TEAL));
        label(card,"Logged spending: "+money(spent,trip.homeCurrency)+" total / "+money(spent/trip.members.size(),trip.homeCurrency)+" each");
        label(card,"Equal split preview only. No payments, balances owed or transfers are created.");
        button(content,"Choose destinations in Discover",()->openFeature("DiscoverActivity"));
    }
}
