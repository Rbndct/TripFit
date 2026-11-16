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
        StringBuilder text=new StringBuilder("TRIPPINGS\n").append(trip.name).append("\n").append(trip.city).append(" · ").append(trip.startDate).append(" · ").append(trip.days).append(" days\n\n")
            .append("PROTOTYPE: sample prices, exchange rates and travel estimates. Verify bookings, opening hours and routing before travel.\n\n");
        for(int day=1;day<=trip.days;day++) {
            text.append("DAY ").append(day).append("\n");
            List<RoutePreview.Slot> slots=RoutePreview.day(trip,day,false);
            if(slots.isEmpty()) text.append("No stops planned yet.\n");
            for(RoutePreview.Slot slot:slots) {
                DraftTrip.Stop stop=slot.stop;
                text.append(RoutePreview.time(slot.start)).append("–").append(RoutePreview.time(slot.end)).append(" ").append(stop.name).append(stop.booked?" [BOOKED]":"").append(slot.conflict?" [CHECK CONFLICT]":"").append("\n");
                text.append("Stay ").append(stop.duration).append(" min; ~").append(slot.travel).append(" min incoming travel; ").append(stop.buffer).append(" min buffer; ").append(stop.mode).append(" transport\n");
                text.append(String.format(Locale.US,"Estimate: %s %.0f (≈ %s %.0f)\n",trip.costCurrency(stop),stop.cost,trip.homeCurrency,trip.convertedCost(stop)));
                if(stop.hasLocation) text.append(String.format(Locale.US,"Pin: %.5f, %.5f\n",stop.lat,stop.lng));
            }
            text.append("\n");
        }
        double planned=0,actual=0;
        for(DraftTrip.Stop stop:trip.stops) planned+=trip.convertedCost(stop);
        for(DraftTrip.Expense expense:trip.expenses) actual+=RoutePreview.convert(expense.amount,expense.currency,trip.homeCurrency);
        text.append(String.format(Locale.US,"BUDGET (%s)\nTrip limit: %.0f\nEstimated stops: %.0f\nLogged expenses: %.0f\nRemaining after expenses: %.0f\n",trip.homeCurrency,trip.totalBudget(),planned,actual,trip.totalBudget()-actual));
        for(String category:new String[]{"Food","Activities","Transport","Other"}) {
            double categoryPlanned=0,categoryActual=0;
            for(DraftTrip.Stop stop:trip.stops) if(stop.category.equals(category)) categoryPlanned+=trip.convertedCost(stop);
            for(DraftTrip.Expense expense:trip.expenses) if(expense.category.equals(category)) categoryActual+=RoutePreview.convert(expense.amount,expense.currency,trip.homeCurrency);
            text.append(String.format(Locale.US,"%s: limit %.0f / planned %.0f / logged %.0f\n",category,trip.categoryBudget(category),categoryPlanned,categoryActual));
        }
        text.append("\nRates: 1 JPY = 0.38 PHP; 1 USD = 58 PHP; 1 EUR = 63 PHP (samples, not live).\nTravel leg costs must be estimated manually in Transport.\n");
        return text.toString();
    }
    public static void pdf(DraftTrip trip,OutputStream output) throws IOException {
        PdfDocument document=new PdfDocument();
        try {
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); paint.setTextSize(11); paint.setColor(Color.rgb(19,33,59));
            List<String> lines=new ArrayList<>();
            for(String paragraph:summary(trip).split("\n",-1)) {
                if(paragraph.isEmpty()) { lines.add(""); continue; }
                while(!paragraph.isEmpty()) {
                    int count=paint.breakText(paragraph,true,515,null);
                    if(count<paragraph.length()) { int space=paragraph.lastIndexOf(' ',count); if(space>0) count=space; }
                    count=Math.max(1,count); lines.add(paragraph.substring(0,count)); paragraph=paragraph.substring(count).trim();
                }
            }
            int number=0;
            for(int offset=0;offset<lines.size();offset+=43) {
                PdfDocument.Page page=document.startPage(new PdfDocument.PageInfo.Builder(595,842,++number).create());
                Canvas canvas=page.getCanvas(); canvas.drawColor(Color.WHITE);
                paint.setTextSize(16); canvas.drawText("Trippings | Trip summary",40,40,paint); paint.setTextSize(11);
                int y=72; for(int i=offset;i<Math.min(offset+43,lines.size());i++) { canvas.drawText(lines.get(i),40,y,paint); y+=16; }
                canvas.drawText("Local prototype · Page "+number,40,810,paint); document.finishPage(page);
            }
            document.writeTo(output);
        } finally {
            document.close();
        }
    }
    public static String calendar(DraftTrip trip) {
        StringBuilder ics=new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Trippings//Local Prototype//EN\r\nCALSCALE:GREGORIAN\r\n");
        SimpleDateFormat stamp=new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'",Locale.US); stamp.setTimeZone(TimeZone.getTimeZone("UTC"));
        for(int day=1;day<=trip.days;day++) for(RoutePreview.Slot slot:RoutePreview.day(trip,day,false)) {
            String address=slot.stop.address.isEmpty()?slot.stop.hasLocation?String.format(Locale.US,"%.5f, %.5f",slot.stop.lat,slot.stop.lng):trip.city:slot.stop.address;
            ics.append("BEGIN:VEVENT\r\nUID:").append(slot.stop.id).append("@trippings.local\r\nDTSTAMP:").append(stamp.format(new Date())).append("\r\n")
                .append("DTSTART:").append(utc(trip,day,slot.start)).append("\r\nDTEND:").append(utc(trip,day,slot.end)).append("\r\n")
                .append(fold("SUMMARY:"+escape(slot.stop.name))).append("\r\n")
                .append(fold("LOCATION:"+escape(address))).append("\r\n")
                .append(fold("DESCRIPTION:"+escape("Trippings prototype. Sample travel estimates; verify this plan before travel."+(slot.stop.booked?" Booked time is fixed.":"")+(slot.conflict?" Schedule conflict: review before import.":"")))).append("\r\nEND:VEVENT\r\n");
        }
        return ics.append("END:VCALENDAR\r\n").toString();
    }
    private static String utc(DraftTrip trip,int day,int minute) {
        try {
            TimeZone zone=TimeZone.getTimeZone(trip.city.equals("Tokyo")?"Asia/Tokyo":"Asia/Manila");
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
