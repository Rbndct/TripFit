package com.itismob.s01.group01.tripfit.ui;

/**
 * One trip card on the Home screen: identity, trip length (from the trip's saved details), stop
 * count, city, ISO start date, sample flag and the formatted total budget ("" when unknown).
 */
public final class HomeTripRow {

    private final String tripId;
    private final String name;
    private final int dayCount;
    private final int stopCount;
    private final String city;
    private final String startDateIso;
    private final boolean sample;
    private final String budgetLabel;

    public HomeTripRow(String tripId, String name, int dayCount, int stopCount, String city,
                       String startDateIso, boolean sample, String budgetLabel) {
        this.tripId = tripId;
        this.name = name;
        this.dayCount = dayCount;
        this.stopCount = stopCount;
        this.city = city;
        this.startDateIso = startDateIso;
        this.sample = sample;
        this.budgetLabel = budgetLabel;
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

    public String getCity() {
        return city;
    }

    public String getStartDateIso() {
        return startDateIso;
    }

    public boolean isSample() {
        return sample;
    }

    public String getBudgetLabel() {
        return budgetLabel;
    }
}
