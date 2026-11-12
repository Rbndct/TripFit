package com.itismob.s01.group01.tripfit.ui;

/** One trip card on the Home screen: trip identity plus its day/stop counts. */
public final class HomeTripRow {

    private final String tripId;
    private final String name;
    private final int dayCount;
    private final int stopCount;

    public HomeTripRow(String tripId, String name, int dayCount, int stopCount) {
        this.tripId = tripId;
        this.name = name;
        this.dayCount = dayCount;
        this.stopCount = stopCount;
    }

    public String getTripId() {
        return tripId;
    }

    public String getName() {
        return name;
    }

    public int getDayCount() {
        return dayCount;
    }

    public int getStopCount() {
        return stopCount;
    }
}
