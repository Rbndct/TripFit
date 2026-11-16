package com.itismob.s01.group01.tripfit.viewmodel;

/** Result of saving trip settings: saved, refused because stops sit on later days, or failed. */
public final class TripUpdateOutcome {

    public enum Status { SAVED, BLOCKED_BY_STOPS, FAILED }

    private final Status status;
    private final int maxStopDay;

    private TripUpdateOutcome(Status status, int maxStopDay) {
        this.status = status;
        this.maxStopDay = maxStopDay;
    }

    public static TripUpdateOutcome saved() {
        return new TripUpdateOutcome(Status.SAVED, 0);
    }

    public static TripUpdateOutcome blocked(int maxStopDay) {
        return new TripUpdateOutcome(Status.BLOCKED_BY_STOPS, maxStopDay);
    }

    public static TripUpdateOutcome failed() {
        return new TripUpdateOutcome(Status.FAILED, 0);
    }

    public Status getStatus() {
        return status;
    }

    public int getMaxStopDay() {
        return maxStopDay;
    }
}
