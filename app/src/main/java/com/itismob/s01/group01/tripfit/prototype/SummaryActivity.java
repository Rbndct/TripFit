package com.itismob.s01.group01.tripfit.prototype;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SummaryActivity extends PrototypeActivity {
    private ActivityResultLauncher<String> pdfExport, calendarExport;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if(trip==null) { finish(); return; }
        pdfExport=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),uri->write(uri,true));
        calendarExport=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/calendar"),uri->write(uri,false));
        render();
    }
    private void render() {
        page("Ready for the adventure",trip.name+" · Calendar & export",true);
        notice("Calendar sync preview · Google sign-in and direct sync are not connected. You can review the events and save a real calendar file to import yourself. PDF export works locally without an account.");
        int conflicts=0;
        for(int day=1;day<=trip.days;day++) for(RoutePreview.Slot slot:RoutePreview.day(trip,day,false)) if(slot.conflict) conflicts++;
        if(conflicts>0) notice(conflicts+" schedule warning(s). Return to the itinerary to check overlapping bookings or activities outside the day limit before importing.");
        button(content,"Preview Google Calendar sync",()->{
            if(trip.stops.isEmpty()) { toast("Add a destination before previewing calendar events"); return; }
            new MaterialAlertDialogBuilder(this).setTitle("Review calendar sync")
                .setMessage(trip.stops.size()+" events would be created, including each stop's time and location.\n\nTimes use "+(trip.city.equals("Tokyo")?"Asia/Tokyo":"Asia/Manila")+".\n\nThis preview does not connect to Google or send events. Export a calendar file below to import it manually.")
                .setNegativeButton("Cancel",null).setPositiveButton("Complete preview",(d,w)->{trip.calendarPreview=true;save();render();}).show();
        });
        if(trip.calendarPreview) label(content,"✓ Preview reviewed · No events have been synced to Google Calendar.");
        button(content,"Save calendar file (.ics)",()->{if(trip.stops.isEmpty()) toast("Add at least one destination first");else calendarExport.launch("Trippings-itinerary.ics");});
        button(content,"Export itinerary & budget as PDF",()->pdfExport.launch("Trippings-trip-summary.pdf"));
        button(content,"Share trip summary",()->{
            Intent share=new Intent(Intent.ACTION_SEND); share.setType("text/plain"); share.putExtra(Intent.EXTRA_SUBJECT,trip.name+" · Trippings"); share.putExtra(Intent.EXTRA_TEXT,TripExport.summary(trip)); startActivity(Intent.createChooser(share,"Share your trip summary"));
        });
        title("Timed event preview");
        for(int day=1;day<=trip.days;day++) {
            title("Day "+day+" · "+date(day));
            for(RoutePreview.Slot slot:RoutePreview.day(trip,day,false)) {
                LinearLayout card=card(content); card.addView(text(slot.stop.name,18,INK));
                label(card,RoutePreview.time(slot.start)+"–"+RoutePreview.time(slot.end)+(slot.stop.booked?" · Booked":""));
                label(card,slot.stop.address.isEmpty()?slot.stop.hasLocation?String.format(java.util.Locale.US,"Pin: %.5f, %.5f",slot.stop.lat,slot.stop.lng):"Location: "+trip.city:slot.stop.address);
                if(slot.conflict) label(card,"⚠ Review this schedule conflict before import.");
            }
        }
    }
    private void write(Uri uri,boolean pdf) {
        if(uri==null) return;
        try(OutputStream output=getContentResolver().openOutputStream(uri)) {
            if(output==null) throw new java.io.IOException("No output stream");
            if(pdf) TripExport.pdf(trip,output); else output.write(TripExport.calendar(trip).getBytes(StandardCharsets.UTF_8));
            toast(pdf?"Trip summary saved":"Calendar file saved. Import it into your calendar when ready.");
        } catch(java.io.IOException|RuntimeException e) { toast("Could not save the file. Choose a different location and try again."); }
    }
}
