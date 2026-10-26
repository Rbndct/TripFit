package com.itismob.s01.group01.tripfit.model;

/** A single destination/stop on the itinerary. UI-only model for Sprint 1; no persistence yet. */
public class ItineraryItem {

    private final String id;
    private final int day;
    private final String name;
    private final String durationLabel;
    private final boolean booked;
    private final String bookedTime;

    public ItineraryItem(String id, int day, String name, String durationLabel,
                          boolean booked, String bookedTime) {
        this.id = id;
        this.day = day;
        this.name = name;
        this.durationLabel = durationLabel;
        this.booked = booked;
        this.bookedTime = bookedTime;
    }

    public String getId() {
        return id;
    }

    public int getDay() {
        return day;
    }

    public String getName() {
        return name;
    }

    public String getDurationLabel() {
        return durationLabel;
    }

    public boolean isBooked() {
        return booked;
    }

    public String getBookedTime() {
        return bookedTime;
    }
}
