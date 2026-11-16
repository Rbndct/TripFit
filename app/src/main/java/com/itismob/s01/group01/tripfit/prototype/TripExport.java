package com.itismob.s01.group01.tripfit.prototype;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** User-initiated local exports. Nothing is sent to a calendar or another account. */
public final class TripExport {
    public static String summary(DraftTrip trip) {
        return summary(trip,new com.itismob.s01.group01.tripfit.data.frontend.SampleTravelDataSource().cached(com.itismob.s01.group01.tripfit.data.frontend.TravelQuery.from(trip)));
    }
    public static String summary(DraftTrip trip,com.itismob.s01.group01.tripfit.data.frontend.TravelSnapshot data) {
        StringBuilder text=new StringBuilder("TRIPPINGS\n").append(trip.name).append("\n").append(trip.city).append(" · ").append(trip.startDate).append(" · ").append(trip.days).append(" days\n\n")
            .append("Travel plan preview. Verify bookings, opening hours and routing before travel.\n").append(data.placeSource).append("\n\n");
        for(int day=1;day<=trip.days;day++) {
            text.append("DAY ").append(day).append("\n");
            List<RoutePreview.Slot> slots=RoutePreview.day(trip,day,false,data.travelTimes);
            DraftTrip.Stop previous=null;
            if(slots.isEmpty()) text.append("No stops planned yet.\n");
            for(RoutePreview.Slot slot:slots) {
                DraftTrip.Stop stop=slot.stop;
                text.append(RoutePreview.time(slot.start)).append("–").append(RoutePreview.time(slot.end)).append(" ").append(stop.name).append(stop.booked?" [BOOKED]":"").append(slot.conflict?" [CHECK CONFLICT]":"").append("\n");
                text.append("Stay ").append(stop.duration).append(" min; ");
                if(previous==null) text.append("start of day\n");
                else text.append(data.travelTimes.mode(previous,stop)).append(" ~").append(slot.travel).append(" min incoming travel + ").append(stop.buffer).append(" min buffer\n");
                if(stop.costKnown) text.append(String.format(Locale.US,"Estimate: %s %.0f (≈ %s %.0f)\n",trip.costCurrency(stop),stop.cost,trip.homeCurrency,trip.convertedCost(stop,data.rates)));
                else text.append("Price estimate unavailable; excluded from planned spending.\n");
                if(stop.hasLocation) text.append(String.format(Locale.US,"Pin: %.5f, %.5f\n",stop.lat,stop.lng));
                text.append("\n"); previous=stop;
            }
            text.append("\n");
        }
        double planned=0,actual=0;
        for(DraftTrip.Stop stop:trip.stops) planned+=trip.convertedCost(stop,data.rates);
        for(DraftTrip.Expense expense:trip.expenses) actual+=data.rates.convert(expense.amount,expense.currency,trip.homeCurrency);
        text.append(String.format(Locale.US,"BUDGET (%s)\nTrip limit: %.0f\nEstimated stops: %.0f\nLogged expenses: %.0f\nRemaining after expenses: %.0f\n",trip.homeCurrency,trip.totalBudget(),planned,actual,trip.totalBudget()-actual));
        for(String category:new String[]{"Food","Activities","Transport","Other"}) {
            double categoryPlanned=0,categoryActual=0;
            for(DraftTrip.Stop stop:trip.stops) if(stop.category.equals(category)) categoryPlanned+=trip.convertedCost(stop,data.rates);
            for(DraftTrip.Expense expense:trip.expenses) if(expense.category.equals(category)) categoryActual+=data.rates.convert(expense.amount,expense.currency,trip.homeCurrency);
            text.append(String.format(Locale.US,"%s: limit %.0f / planned %.0f / logged %.0f\n",category,trip.categoryBudget(category),categoryPlanned,categoryActual));
        }
        text.append("\n").append(data.rates.description).append("\n").append(data.routeSource).append("\nTravel leg costs must be estimated manually in Transport.\n");
        return text.toString();
    }
    public static void pdf(DraftTrip trip,OutputStream output) throws IOException {
        pdf(trip,output,new com.itismob.s01.group01.tripfit.data.frontend.SampleTravelDataSource().cached(com.itismob.s01.group01.tripfit.data.frontend.TravelQuery.from(trip)));
    }
    public static void pdf(DraftTrip trip,OutputStream output,com.itismob.s01.group01.tripfit.data.frontend.TravelSnapshot data) throws IOException {
        PdfDocument document=new PdfDocument();
        try {
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); paint.setTextSize(11); paint.setColor(Color.rgb(19,33,59));
            List<List<String>> pages=new ArrayList<>();
            List<String> current=new ArrayList<>(); pages.add(current);
            for(String block:summary(trip,data).split("\n\n+")) {
                List<String> wrapped=new ArrayList<>();
                for(String paragraph:block.split("\n")) {
                    while(!paragraph.isEmpty()) {
                        int count=paint.breakText(paragraph,true,515,null);
                        if(count<paragraph.length()) { int space=paragraph.lastIndexOf(' ',count); if(space>0) count=space; }
                        count=Math.max(1,count); wrapped.add(paragraph.substring(0,count)); paragraph=paragraph.substring(count).trim();
                    }
                }
                // Keep each stop and the category-budget breakdown together when possible.
                if(!current.isEmpty() && current.size()+wrapped.size()>43) { current=new ArrayList<>(); pages.add(current); }
                for(String line:wrapped) {
                    if(current.size()>=43) { current=new ArrayList<>(); pages.add(current); }
                    current.add(line);
                }
                if(current.size()<43) current.add("");
            }
            int number=0;
            for(List<String> lines:pages) {
                PdfDocument.Page page=document.startPage(new PdfDocument.PageInfo.Builder(595,842,++number).create());
                Canvas canvas=page.getCanvas(); canvas.drawColor(Color.WHITE);
                paint.setTextSize(16); canvas.drawText("Trippings | Trip summary",40,40,paint); paint.setTextSize(11);
                int y=72; for(String line:lines) { canvas.drawText(line,40,y,paint); y+=16; }
                canvas.drawText("Local prototype · Page "+number,40,810,paint); document.finishPage(page);
            }
            document.writeTo(output);
        } finally {
            document.close();
        }
    }
    public static String calendar(DraftTrip trip) {
        return calendar(trip,new com.itismob.s01.group01.tripfit.data.frontend.SampleTravelDataSource().cached(com.itismob.s01.group01.tripfit.data.frontend.TravelQuery.from(trip)));
    }
    public static String calendar(DraftTrip trip,com.itismob.s01.group01.tripfit.data.frontend.TravelSnapshot data) {
        StringBuilder ics=new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Trippings//Local Prototype//EN\r\nCALSCALE:GREGORIAN\r\n");
        SimpleDateFormat stamp=new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'",Locale.US); stamp.setTimeZone(TimeZone.getTimeZone("UTC"));
        for(int day=1;day<=trip.days;day++) for(RoutePreview.Slot slot:RoutePreview.day(trip,day,false,data.travelTimes)) {
            String address=slot.stop.address.isEmpty()?slot.stop.hasLocation?String.format(Locale.US,"%.5f, %.5f",slot.stop.lat,slot.stop.lng):trip.city:slot.stop.address;
            ics.append("BEGIN:VEVENT\r\nUID:").append(slot.stop.id).append("@trippings.local\r\nDTSTAMP:").append(stamp.format(new Date())).append("\r\n")
                .append("DTSTART:").append(utc(trip,day,slot.start)).append("\r\nDTEND:").append(utc(trip,day,slot.end)).append("\r\n")
                .append(fold("SUMMARY:"+escape(slot.stop.name))).append("\r\n")
                .append(fold("LOCATION:"+escape(address))).append("\r\n")
                .append(fold("DESCRIPTION:"+escape("Trippings travel plan. "+data.routeSource+" Verify this plan before travel."+(slot.stop.booked?" Booked time is fixed.":"")+(slot.conflict?" Schedule conflict: review before import.":"")))).append("\r\nEND:VEVENT\r\n");
        }
        return ics.append("END:VCALENDAR\r\n").toString();
    }
    private static String utc(DraftTrip trip,int day,int minute) {
        try {
            TimeZone zone=TimeZone.getTimeZone(trip.timeZone());
            SimpleDateFormat date=new SimpleDateFormat("yyyy-MM-dd",Locale.US); date.setLenient(false); date.setTimeZone(zone);
            Calendar calendar=Calendar.getInstance(zone); calendar.setTime(date.parse(trip.startDate)); calendar.add(Calendar.DATE,day-1); calendar.add(Calendar.MINUTE,minute);
            SimpleDateFormat output=new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'",Locale.US); output.setTimeZone(TimeZone.getTimeZone("UTC")); return output.format(calendar.getTime());
        } catch(java.text.ParseException e) { throw new IllegalArgumentException("Invalid trip date",e); }
    }
    private static String escape(String text) { return text.replace("\\","\\\\").replace("\n","\\n").replace("\r","").replace(";","\\;").replace(",","\\,"); }
    private static String fold(String line) {
        StringBuilder result=new StringBuilder(); int bytes=0;
        for(int i=0;i<line.length();) {
            int codePoint=line.codePointAt(i); String value=new String(Character.toChars(codePoint)); int length=value.getBytes(StandardCharsets.UTF_8).length;
            if(bytes+length>73) { result.append("\r\n "); bytes=1; } result.append(value); bytes+=length; i+=Character.charCount(codePoint);
        }
        return result.toString();
    }
    private TripExport() {}
}
