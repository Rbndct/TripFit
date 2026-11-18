package com.itismob.s01.group01.tripfit.prototype;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** User-initiated local exports. Nothing is sent to a calendar or another account. */
public final class TripExport {
    public static String summary(DraftTrip trip) {
        StringBuilder text=new StringBuilder("TRIPPINGS\n").append(trip.name).append("\n").append(trip.city).append(" · ").append(TripFormRules.isValidIsoDate(trip.startDate) ? trip.startDate : "Dates not set").append(" · ").append(trip.days).append(" days\n\n")
            .append("PROTOTYPE: sample prices, exchange rates and travel estimates. Verify bookings, opening hours and routing before travel.\n\n");
        for(int day=1;day<=trip.days;day++) {
            text.append("DAY ").append(day).append("\n");
            List<RoutePreview.Slot> slots=RoutePreview.day(trip,day,false);
            DraftTrip.Stop previous=null;
            if(slots.isEmpty()) text.append("No stops planned yet.\n");
            for(RoutePreview.Slot slot:slots) {
                DraftTrip.Stop stop=slot.stop;
                text.append(RoutePreview.time(slot.start)).append("–").append(RoutePreview.time(slot.end)).append(" ").append(stop.name).append(stop.booked?" [BOOKED]":"").append(slot.conflict?" [CHECK CONFLICT]":"").append("\n");
                text.append("Stay ").append(stop.duration).append(" min; ");
                if(previous==null) text.append("start of day\n");
                else text.append(RoutePreview.mode(previous,stop)).append(" ~").append(slot.travel).append(" min incoming travel + ").append(stop.buffer).append(" min buffer\n");
                text.append(String.format(Locale.US,"Estimate: %s %.2f (≈ %s %.2f)\n",trip.costCurrency(stop),stop.cost,trip.homeCurrency,trip.convertedCost(stop)));
                if(stop.hasLocation) text.append(String.format(Locale.US,"Pin: %.5f, %.5f\n",stop.lat,stop.lng));
                text.append("\n"); previous=stop;
            }
            text.append("\n");
        }
        double planned=0,actual=0;
        for(DraftTrip.Stop stop:trip.stops) planned+=trip.convertedCost(stop);
        for(DraftTrip.Expense expense:trip.expenses) actual+=RoutePreview.convert(expense.amount,expense.currency,trip.homeCurrency);
        text.append(String.format(Locale.US,"BUDGET (%s)\nTrip limit: %.2f\nEstimated stops: %.2f\nLogged expenses: %.2f\nRemaining after expenses: %.2f\n",trip.homeCurrency,trip.totalBudget(),planned,actual,trip.totalBudget()-actual));
        for(String category:new String[]{"Food","Activities","Transport","Other"}) {
            double categoryPlanned=0,categoryActual=0;
            for(DraftTrip.Stop stop:trip.stops) if(stop.category.equals(category)) categoryPlanned+=trip.convertedCost(stop);
            for(DraftTrip.Expense expense:trip.expenses) if(expense.category.equals(category)) categoryActual+=RoutePreview.convert(expense.amount,expense.currency,trip.homeCurrency);
            text.append(String.format(Locale.US,"%s: limit %.2f / planned %.2f / logged %.2f\n",category,trip.categoryBudget(category),categoryPlanned,categoryActual));
        }
        text.append("\n").append(ratesLine(trip.homeCurrency)).append("\nTravel leg costs must be estimated manually in Transport.\n");
        return text.toString();
    }
    /** The rates footer, generated from the app's single rates table (sample or live wording). */
    static String ratesLine(String home) {
        StringBuilder line=new StringBuilder(CurrencyRates.isLive()
            ? "Rates as of "+CurrencyRates.asOfDate()+" (reference rates, not a bank quote): "
            : "Rates (samples, not live): ");
        boolean first=true;
        for(String code:new String[]{"PHP","JPY","USD","EUR"}) {
            if(code.equals(home) || !CurrencyRates.has(code) || !CurrencyRates.has(home)) continue;
            line.append(first?"":"; ").append(String.format(Locale.US,"1 %s = %.4f %s",code,CurrencyRates.convert(1,code,home),home));
            first=false;
        }
        return line.append(first?"no conversions needed.":".").toString();
    }
    public static void pdf(DraftTrip trip,OutputStream output) throws IOException {
        PdfDocument document=new PdfDocument();
        try {
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); paint.setTextSize(11); paint.setColor(Color.rgb(19,33,59));
            List<List<String>> pages=new ArrayList<>();
            List<String> current=new ArrayList<>(); pages.add(current);
            for(String block:summary(trip).split("\n\n+")) {
                List<String> wrapped=new ArrayList<>();
                for(String paragraph:block.split("\n")) {
                    while(!paragraph.isEmpty()) {
                        int count=paint.breakText(paragraph,true,515,null);
                        if(count<paragraph.length()) { int space=paragraph.lastIndexOf(' ',count); if(space>0) count=space; }
                        if (count > 0 && count < paragraph.length()
                                && Character.isSurrogatePair(paragraph.charAt(count - 1), paragraph.charAt(count))) {
                            count--;
                        }
                        count = Math.max(Character.charCount(paragraph.codePointAt(0)), count);
                        wrapped.add(paragraph.substring(0, count));
                        paragraph = paragraph.substring(count).trim();
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
    public static String calendarZoneId(String city) {
        switch (city) {
            case "Tokyo": return "Asia/Tokyo";
            case "Manila": return "Asia/Manila";
            case "Seoul": return "Asia/Seoul";
            case "Singapore": return "Asia/Singapore";
            case "Paris": return "Europe/Paris";
            case "New York": return "America/New_York";
            default: return "UTC";
        }
    }

    public static String location(DraftTrip trip, DraftTrip.Stop stop) {
        if (stop.address != null && !stop.address.trim().isEmpty()) {
            return stop.address.trim();
        }
        return stop.hasLocation ? String.format(Locale.US, "%.5f, %.5f", stop.lat, stop.lng)
                : trip.city;
    }

    public static String calendar(DraftTrip trip) {
        if (!TripFormRules.isValidIsoDate(trip.startDate)) {
            throw new IllegalArgumentException("Invalid trip date");
        }
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone(calendarZoneId(trip.city)), Locale.US);
        start.clear();
        start.set(Integer.parseInt(trip.startDate.substring(0, 4)),
                Integer.parseInt(trip.startDate.substring(5, 7)) - 1,
                Integer.parseInt(trip.startDate.substring(8, 10)));
        SimpleDateFormat stamp = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
        stamp.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = stamp.format(new Date());

        StringBuilder ics = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n")
                .append("PRODID:-//Trippings//Local Prototype//EN\r\nCALSCALE:GREGORIAN\r\n");
        for (int day = 1; day <= trip.days; day++) {
            for (RoutePreview.Slot slot : RoutePreview.day(trip, day, false)) {
                Date eventStart = localTime(start, day, slot.start);
                Date eventEnd = localTime(start, day, slot.end);
                if (!eventEnd.after(eventStart)) {
                    // A skipped local hour can move the start past the scheduled end.
                    eventEnd = new Date(eventStart.getTime() + (slot.end - slot.start) * 60_000L);
                }
                ics.append("BEGIN:VEVENT\r\n")
                        .append(fold("UID:" + escape(slot.stop.id) + "@trippings.local")).append("\r\n")
                        .append("DTSTAMP:").append(timestamp).append("\r\n")
                        .append("DTSTART:").append(stamp.format(eventStart)).append("\r\n")
                        .append("DTEND:").append(stamp.format(eventEnd)).append("\r\n")
                        .append(fold("SUMMARY:" + escape(slot.stop.name))).append("\r\n")
                        .append(fold("LOCATION:" + escape(location(trip, slot.stop)))).append("\r\n")
                        .append(fold("DESCRIPTION:" + escape(eventDescription(slot))))
                        .append("\r\nEND:VEVENT\r\n");
            }
        }
        return ics.append("END:VCALENDAR\r\n").toString();
    }

    private static String eventDescription(RoutePreview.Slot slot) {
        return "Trippings prototype. Sample travel estimates; verify this plan before travel."
                + (slot.stop.booked ? " Booked time is fixed." : "")
                + (slot.conflict ? " Schedule conflict: review before import." : "");
    }

    private static Date localTime(Calendar start, int day, int minute) {
        Calendar local = (Calendar) start.clone();
        local.add(Calendar.DATE, day - 1 + Math.floorDiv(minute, 1440));
        int wallTime = Math.floorMod(minute, 1440);
        local.set(Calendar.HOUR_OF_DAY, wallTime / 60);
        local.set(Calendar.MINUTE, wallTime % 60);
        return local.getTime();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\r\n", "\n")
                .replace("\r", "\n").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,");
    }

    private static String fold(String line) {
        StringBuilder result = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            String value = new String(Character.toChars(codePoint));
            int length = value.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + length > 75) {
                result.append("\r\n ");
                bytes = 1;
            }
            result.append(value);
            bytes += length;
            i += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private TripExport() { }
}
