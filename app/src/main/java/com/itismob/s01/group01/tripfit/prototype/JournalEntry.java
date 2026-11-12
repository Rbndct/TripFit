package com.itismob.s01.group01.tripfit.prototype;

/** A note (and optionally a photo marker) attached to a day/stop. */
public final class JournalEntry {

    private final int day;
    private final String stopName;
    private final String note;
    private final boolean hasPhoto;

    public JournalEntry(int day, String stopName, String note, boolean hasPhoto) {
        this.day = day;
        this.stopName = stopName;
        this.note = note;
        this.hasPhoto = hasPhoto;
    }

    public int getDay() {
        return day;
    }

    public String getStopName() {
        return stopName;
    }

    public String getNote() {
        return note;
    }

    public boolean hasPhoto() {
        return hasPhoto;
    }
}
