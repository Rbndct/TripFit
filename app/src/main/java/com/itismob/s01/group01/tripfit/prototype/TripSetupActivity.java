package com.itismob.s01.group01.tripfit.prototype;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.textfield.TextInputEditText;
import com.itismob.s01.group01.tripfit.HomeActivity;
import java.util.Calendar;
import java.util.Locale;

public class TripSetupActivity extends PrototypeActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean editing=trip!=null;
        if(!editing) { trip=new DraftTrip(); trip.startDate=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date()); trip.members.add("You"); }
        page(editing?"Trip settings":"A new adventure","A few details, then make it yours.",true);
        TextInputEditText name=field(content,"Trip name",trip.name,InputType.TYPE_CLASS_TEXT);
        Spinner city=choices(content,"Destination city",new String[]{"Tokyo","Manila"},trip.city);
        label(content,"Prototype cities: Tokyo and Manila. You can add a pin anywhere on the map.");
        TextInputEditText start=field(content,"Start date (YYYY-MM-DD)",trip.startDate,InputType.TYPE_CLASS_DATETIME);
        button(content,"Choose start date",()->{
            Calendar calendar=Calendar.getInstance();
            new DatePickerDialog(this,(picker,year,month,day)->start.setText(String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day)),calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        TextInputEditText days=field(content,"Number of days",String.valueOf(trip.days),InputType.TYPE_CLASS_NUMBER);
        Spinner currency=choices(content,"Home currency",new String[]{"PHP","JPY","USD","EUR"},trip.homeCurrency);
        TextInputEditText budget=field(content,"Budget in home currency",String.valueOf(trip.budget),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        CheckBox daily=check(content,"This is a daily budget",trip.dailyBudget);
        if(editing) label(content,"Changing city keeps your existing stops. Changing currency keeps the numeric budget; review category limits in Budget.");
        button(content,editing?"Save trip settings":"Create trip",()->{
            if(value(name).isEmpty()) { name.setError("Give your trip a name"); return; }
            try {
                java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US); format.setLenient(false);
                if(!value(start).matches("\\d{4}-\\d{2}-\\d{2}")) throw new java.text.ParseException("date",0);
                format.parse(value(start));
            } catch(java.text.ParseException e) { start.setError("Use a valid date, for example 2026-12-01"); return; }
            try {
                int count=(int)number(days,1,30); double amount=number(budget,1,100000000);
                for(DraftTrip.Stop stop:trip.stops) if(stop.day>count) { days.setError("Move stops from later days before shortening this trip"); return; }
                trip.name=value(name); trip.city=city.getSelectedItem().toString(); trip.startDate=value(start); trip.days=count;
                trip.homeCurrency=currency.getSelectedItem().toString(); trip.dailyBudget=daily.isChecked();
                if(trip.budget!=amount || !editing) { trip.foodBudget=amount*.3; trip.activityBudget=amount*.4; trip.transportBudget=amount*.2; trip.otherBudget=amount*.1; }
                trip.budget=amount; store.save(trip);
                if(!editing) { Intent intent=new Intent(this,PlannerActivity.class); intent.putExtra(HomeActivity.EXTRA_TRIP_ID,trip.id); startActivity(intent); }
                finish();
            } catch(IllegalArgumentException ignored) {}
        });
    }
}
