package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.CurrencyRates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The single planning engine: deterministic frontend estimates, not street routing or live
 * transit times. Booked stops are anchors that never move; flexible stops flow around them.
 */
public final class RoutePreview {
    public static class Slot {
        public DraftTrip.Stop stop;
        public int start, end, travel;
        public boolean conflict;
        Slot(DraftTrip.Stop stop,int start,int travel,boolean conflict) {
            this.stop=stop; this.start=start; this.end=start+stop.duration; this.travel=travel; this.conflict=conflict;
        }
    }

    /** Current order versus the nearest-neighbour preview for one day. */
    public static final class Comparison {
        public final List<Slot> current, preview;
        public final int currentTravel, previewTravel;
        public final boolean orderChanged;
        Comparison(List<Slot> current,List<Slot> preview) {
            this.current=Collections.unmodifiableList(current);
            this.preview=Collections.unmodifiableList(preview);
            this.currentTravel=totalTravel(current);
            this.previewTravel=totalTravel(preview);
            this.orderChanged=differsInOrder(current,preview);
        }
    }

    /**
     * One day's schedule. With optimize the nearest-neighbour preview is returned only when it
     * travels no longer and adds no conflicts; otherwise the current order is kept.
     */
    public static List<Slot> day(DraftTrip trip,int day,boolean optimize) {
        List<Slot> current=plan(trip,day,false);
        if(!optimize) return current;
        List<Slot> preview=plan(trip,day,true);
        boolean worse=totalTravel(preview)>totalTravel(current) || conflicts(preview)>conflicts(current);
        return worse?current:preview;
    }

    private static int conflicts(List<Slot> slots) {
        int count=0;
        for(Slot slot:slots) if(slot.conflict) count++;
        return count;
    }

    private static List<Slot> plan(DraftTrip trip,int day,boolean optimize) {
        List<DraftTrip.Stop> flexible=new ArrayList<>(), fixed=new ArrayList<>();
        for(DraftTrip.Stop stop:trip.stops) if(stop.day==day) (stop.booked?fixed:flexible).add(stop);
        fixed.sort(Comparator.comparingInt(s->s.fixedMinute));
        List<Slot> result=new ArrayList<>();
        int cursor=ScheduleMath.DAY_START_MIN;
        DraftTrip.Stop previous=null;
        for(int anchor=0;anchor<=fixed.size();anchor++) {
            DraftTrip.Stop booked=anchor<fixed.size()?fixed.get(anchor):null;
            while(!flexible.isEmpty()) {
                DraftTrip.Stop chosen=pickNext(flexible,previous,booked,cursor,optimize);
                if(chosen==null) break;
                int minutes=travel(previous,chosen);
                int start=cursor+minutes+(previous==null?0:chosen.buffer);
                Slot slot=new Slot(chosen,start,minutes,start+chosen.duration>ScheduleMath.LATE_END_MIN);
                result.add(slot); cursor=slot.end; previous=chosen; flexible.remove(chosen);
            }
            if(booked!=null) {
                int minutes=travel(previous,booked);
                boolean arrivesLate=previous!=null && cursor+minutes+booked.buffer>booked.fixedMinute;
                Slot slot=new Slot(booked,booked.fixedMinute,minutes,arrivesLate || booked.fixedMinute+booked.duration>ScheduleMath.DAY_END_MIN);
                result.add(slot); cursor=Math.max(cursor,slot.end); previous=booked;
            }
        }
        return result;
    }

    /** The next flexible stop that still fits before {@code booked}; nearest-first when optimizing. */
    private static DraftTrip.Stop pickNext(List<DraftTrip.Stop> flexible,DraftTrip.Stop previous,DraftTrip.Stop booked,int cursor,boolean optimize) {
        DraftTrip.Stop chosen=null;
        double nearest=Double.MAX_VALUE;
        boolean chosenPinned=false;
        for(DraftTrip.Stop candidate:flexible) {
            int arrival=cursor+travel(previous,candidate)+(previous==null?0:candidate.buffer);
            int departure=arrival+candidate.duration;
            if(booked!=null && departure+travel(candidate,booked)+booked.buffer>booked.fixedMinute) continue;
            boolean measurable=previous!=null && previous.hasLocation && candidate.hasLocation;
            double distance=measurable?distance(previous.lat,previous.lng,candidate.lat,candidate.lng):0;
            // Pinned candidates outrank unpinned ones (no coordinates); ties keep the original order.
            boolean better=chosen==null || (optimize && ((candidate.hasLocation && !chosenPinned) || (candidate.hasLocation==chosenPinned && distance<nearest)));
            if(better) { chosen=candidate; nearest=distance; chosenPinned=candidate.hasLocation; }
            if(!optimize) break;
        }
        return chosen;
    }

    /** Sum of incoming-leg minutes over a day's slots. */
    public static int totalTravel(List<Slot> slots) {
        int total=0;
        for(Slot slot:slots) total+=slot.travel;
        return total;
    }

    /** Current order (optimize=false) versus preview order (optimize=true); never mutates the trip. */
    public static Comparison compare(DraftTrip trip,int day) {
        return new Comparison(day(trip,day,false),day(trip,day,true));
    }

    /**
     * New day for every FLEXIBLE stop so loads (duration + buffer) even out across all trip days.
     * Booked stops never move and count toward their day's load when it is inside 1..days.
     */
    public static Map<String,Integer> balance(DraftTrip trip) {
        Map<String,Integer> result=new LinkedHashMap<>();
        int days=Math.max(1,trip.days);
        int[] loads=new int[days];
        for(DraftTrip.Stop s:trip.stops) if(s.booked && s.day>=1 && s.day<=days) loads[s.day-1]+=s.duration+s.buffer;
        for(DraftTrip.Stop s:trip.stops) {
            if(s.booked) continue;
            int lowest=0;
            for(int i=1;i<days;i++) if(loads[i]<loads[lowest]) lowest=i;
            loads[lowest]+=s.duration+s.buffer;
            result.put(s.id,lowest+1);
        }
        return result;
    }

    private static boolean differsInOrder(List<Slot> a,List<Slot> b) {
        if(a.size()!=b.size()) return true;
        for(int i=0;i<a.size();i++) if(a.get(i).stop!=b.get(i).stop) return true;
        return false;
    }

    public static int travel(DraftTrip.Stop from,DraftTrip.Stop to) {
        if(from==null) return 0;
        if(!from.hasLocation || !to.hasLocation) return ScheduleMath.PLACEHOLDER_TRAVEL_MIN;
        return resolve(from,to).minutesFor(distance(from.lat,from.lng,to.lat,to.lng));
    }
    public static String mode(DraftTrip.Stop from,DraftTrip.Stop to) {
        return TripSnapshots.modeName(resolve(from,to));
    }
    /** The mode a leg actually uses: the destination's explicit mode, else Suggested by distance. */
    public static TravelMode legMode(DraftTrip.Stop from,DraftTrip.Stop to) { return resolve(from,to); }
    /** Explicit mode on the destination wins; otherwise Suggested by straight-line distance. */
    private static TravelMode resolve(DraftTrip.Stop from,DraftTrip.Stop to) {
        TravelMode explicit=TripSnapshots.toMode(to.mode);
        if(explicit!=null) return explicit;
        if(from==null || !from.hasLocation || !to.hasLocation) return TravelMode.WALK;
        return TravelMode.suggestFor(distance(from.lat,from.lng,to.lat,to.lng));
    }
    public static double distance(double lat1,double lng1,double lat2,double lng2) {
        double a=Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lng2-lng1)/2),2);
        return 6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(Math.max(0,1-a)));
    }
    public static String time(int minute) { return String.format(Locale.US,"%02d:%02d",minute/60,minute%60); }
    /** Converts with the app's single rates table; unknown currency codes pass through unchanged. */
    public static double convert(double amount,String from,String to) {
        return CurrencyRates.convert(amount,from,to);
    }
    private RoutePreview() {}
}
