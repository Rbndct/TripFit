package com.itismob.s01.group01.tripfit.ui;

import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import com.itismob.s01.group01.tripfit.prototype.RoutePreview;
import com.itismob.s01.group01.tripfit.prototype.TravelMode;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One stop card in the active day, in schedule order, with its computed times and incoming leg.
 * Pure data (no Android types) so the composition is unit-testable.
 */
public final class ItineraryRow {

    private final String id;
    private final String name;
    private final int indexInDay;
    private final int start;
    private final int end;
    private final boolean booked;
    private final boolean conflict;
    private final boolean hasLocation;
    private final double lat;
    private final double lng;
    private final boolean hasIncomingLeg;
    private final int incomingTravel;
    private final int buffer;
    private final Integer explicitBuffer;
    private final boolean placeholderLeg;
    private final TravelMode legMode;
    private final TravelMode explicitMode;

    private ItineraryRow(RoutePreview.Slot slot, RoutePreview.Slot previous, int indexInDay) {
        DraftTrip.Stop stop = slot.stop;
        DraftTrip.Stop before = previous == null ? null : previous.stop;
        this.id = stop.id;
        this.name = stop.name;
        this.indexInDay = indexInDay;
        this.start = slot.start;
        this.end = slot.end;
        this.booked = stop.booked;
        this.conflict = slot.conflict;
        this.hasLocation = stop.hasLocation;
        this.lat = stop.lat;
        this.lng = stop.lng;
        this.hasIncomingLeg = before != null;
        this.incomingTravel = slot.travel;
        this.buffer = stop.buffer;
        this.explicitBuffer = stop.explicitBuffer;
        this.placeholderLeg = before != null && (!before.hasLocation || !stop.hasLocation);
        this.legMode = before == null ? null : RoutePreview.legMode(before, stop);
        this.explicitMode = TripSnapshots.toMode(stop.mode);
    }

    /** The day's rows in current (non-optimized) schedule order. */
    public static List<ItineraryRow> build(DraftTrip trip, int day) {
        List<RoutePreview.Slot> slots = RoutePreview.day(trip, day, false);
        List<ItineraryRow> rows = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            rows.add(new ItineraryRow(slots.get(i), i == 0 ? null : slots.get(i - 1), i + 1));
        }
        return Collections.unmodifiableList(rows);
    }

    /** Minutes spent at the stops themselves (end minus start). */
    public static int stayMinutes(List<ItineraryRow> rows) {
        int total = 0;
        for (ItineraryRow row : rows) {
            total += row.end - row.start;
        }
        return total;
    }

    /** Minutes of incoming travel across the day (buffers are not included). */
    public static int travelMinutes(List<ItineraryRow> rows) {
        int total = 0;
        for (ItineraryRow row : rows) {
            total += row.incomingTravel;
        }
        return total;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public int getIndexInDay() { return indexInDay; }

    public int getStart() { return start; }

    public int getEnd() { return end; }

    public boolean isBooked() { return booked; }

    public boolean isConflict() { return conflict; }

    public boolean hasLocation() { return hasLocation; }

    public double getLat() { return lat; }

    public double getLng() { return lng; }

    /** False for the day's first stop: it has no travel and no buffer. */
    public boolean hasIncomingLeg() { return hasIncomingLeg; }

    public int getIncomingTravel() { return incomingTravel; }

    public int getBuffer() { return buffer; }

    /** The stored buffer, or null when the stop follows the profile default. */
    public Integer getExplicitBuffer() { return explicitBuffer; }

    /** True when either end of the incoming leg has no map pin (the 20 min placeholder applies). */
    public boolean isPlaceholderLeg() { return placeholderLeg; }

    /** The mode the incoming leg uses (explicit or Suggested by distance); null without a leg. */
    public TravelMode getLegMode() { return legMode; }

    /** The mode stored for this stop's incoming leg; null means Suggested. */
    public TravelMode getExplicitMode() { return explicitMode; }
}
