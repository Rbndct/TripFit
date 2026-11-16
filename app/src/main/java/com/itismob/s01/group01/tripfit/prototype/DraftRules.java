package com.itismob.s01.group01.tripfit.prototype;

/** Small, testable itinerary rules independent of Android and network services. */
public final class DraftRules {
    public static int defaultDuration(String category) {
        return category.equals("Transport") || category.equals("Other")?30:60;
    }
    public static int journalDay(DraftTrip trip,DraftTrip.Entry entry) {
        for(DraftTrip.Stop stop:trip.stops) if(stop.id.equals(entry.stopId)) return stop.day;
        return entry.day;
    }
    public static void balanceDays(DraftTrip trip) {
        int[] loads=new int[trip.days];
        for(DraftTrip.Stop stop:trip.stops) if(stop.booked) loads[stop.day-1]+=stop.duration+stop.buffer;
        for(DraftTrip.Stop stop:trip.stops) if(!stop.booked) {
            int lowest=0; for(int i=1;i<loads.length;i++) if(loads[i]<loads[lowest]) lowest=i;
            stop.day=lowest+1; loads[lowest]+=stop.duration+stop.buffer;
        }
    }
    private DraftRules() {}
}
