package com.itismob.s01.group01.tripfit.prototype;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Deterministic frontend estimates; these are not street routing or live transit times. */
public final class RoutePreview {
    public static class Slot {
        public DraftTrip.Stop stop;
        public int start, end, travel;
        public boolean conflict;
        Slot(DraftTrip.Stop stop,int start,int travel,boolean conflict) {
            this.stop=stop; this.start=start; this.end=start+stop.duration; this.travel=travel; this.conflict=conflict;
        }
    }
    public static List<Slot> day(DraftTrip trip,int day,boolean optimize) {
        return day(trip,day,optimize,new com.itismob.s01.group01.tripfit.data.frontend.SampleTravelTimes());
    }
    public static List<Slot> day(DraftTrip trip,int day,boolean optimize,
            com.itismob.s01.group01.tripfit.data.frontend.TravelTimeProvider timings) {
        List<Slot> baseline=plan(trip,day,false,timings);
        if(!optimize) return baseline;
        List<Slot> preview=plan(trip,day,true,timings);
        int before=0,after=0,oldConflicts=0,newConflicts=0;
        for(Slot slot:baseline) { before+=slot.travel; if(slot.conflict) oldConflicts++; }
        for(Slot slot:preview) { after+=slot.travel; if(slot.conflict) newConflicts++; }
        return after<=before && newConflicts<=oldConflicts?preview:baseline;
    }
    private static List<Slot> plan(DraftTrip trip,int day,boolean optimize,
            com.itismob.s01.group01.tripfit.data.frontend.TravelTimeProvider timings) {
        List<DraftTrip.Stop> flexible=new ArrayList<>(), fixed=new ArrayList<>();
        for(DraftTrip.Stop stop:trip.stops) if(stop.day==day) (stop.booked?fixed:flexible).add(stop);
        fixed.sort(Comparator.comparingInt(s->s.fixedMinute));
        List<Slot> result=new ArrayList<>();
        int cursor=540;
        DraftTrip.Stop previous=null;
        for(int anchor=0;anchor<=fixed.size();anchor++) {
            DraftTrip.Stop booked=anchor<fixed.size()?fixed.get(anchor):null;
            while(!flexible.isEmpty()) {
                DraftTrip.Stop chosen=null;
                double nearest=Double.MAX_VALUE;
                for(DraftTrip.Stop candidate:flexible) {
                    int arrival=cursor+timings.minutes(previous,candidate)+(previous==null?0:candidate.buffer);
                    int departure=arrival+candidate.duration;
                    if(booked!=null && departure+timings.minutes(candidate,booked)+booked.buffer>booked.fixedMinute) continue;
                    double distance=timings.minutes(previous,candidate);
                    if(chosen==null || (optimize && distance<nearest)) { chosen=candidate; nearest=distance; }
                    if(!optimize) break;
                }
                if(chosen==null) break;
                int minutes=timings.minutes(previous,chosen);
                int start=cursor+minutes+(previous==null?0:chosen.buffer);
                Slot slot=new Slot(chosen,start,minutes,start+chosen.duration>1200);
                result.add(slot); cursor=slot.end; previous=chosen; flexible.remove(chosen);
            }
            if(booked!=null) {
                int minutes=timings.minutes(previous,booked);
                int earliest=cursor+minutes+(previous==null?0:booked.buffer);
                Slot slot=new Slot(booked,booked.fixedMinute,minutes,earliest>booked.fixedMinute || booked.fixedMinute+booked.duration>1440);
                result.add(slot); cursor=Math.max(cursor,slot.end); previous=booked;
            }
        }
        return result;
    }
    public static int travel(DraftTrip.Stop from,DraftTrip.Stop to) {
        if(from==null) return 0;
        if(!from.hasLocation || !to.hasLocation) return 20;
        double km=distance(from.lat,from.lng,to.lat,to.lng)*1.25;
        String mode=mode(from,to);
        double speed=mode.equals("Walking")?4.5:mode.equals("Cycling")?15:mode.equals("Transit")?18:25;
        return Math.max(1,(int)Math.ceil(km/speed*60)+(mode.equals("Transit")?8:0));
    }
    public static String mode(DraftTrip.Stop from,DraftTrip.Stop to) {
        if(!to.mode.equals("Suggested")) return to.mode;
        if(from==null || !from.hasLocation || !to.hasLocation) return "Walking";
        double km=distance(from.lat,from.lng,to.lat,to.lng);
        return km<1.2?"Walking":km<4?"Cycling":"Transit";
    }
    public static double distance(double lat1,double lng1,double lat2,double lng2) {
        double a=Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lng2-lng1)/2),2);
        return 6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(Math.max(0,1-a)));
    }
    public static String time(int minute) { return String.format(Locale.US,"%02d:%02d",minute/60,minute%60); }
    public static double convert(double amount,String from,String to) {
        return com.itismob.s01.group01.tripfit.data.frontend.ExchangeRates.samples().convert(amount,from,to);
    }
    private RoutePreview() {}
}
