package com.itismob.s01.group01.tripfit.prototype;

/**
 * Per-leg transport modes with the demo speeds the planner uses (not live routing). Minutes are
 * computed from straight-line km scaled by {@link #ROAD_FACTOR}.
 */
public enum TravelMode {
    WALK(4.5, 0),
    CYCLE(15.0, 0),
    DRIVE(25.0, 0),
    TRANSIT(18.0, 8);

    /** Straight-line km are multiplied by this to approximate road distance. */
    public static final double ROAD_FACTOR = 1.25;
    private static final double WALK_BELOW_KM = 1.2;
    private static final double CYCLE_BELOW_KM = 4.0;

    private final double speedKmh;
    private final int overheadMin;

    TravelMode(double speedKmh, int overheadMin) {
        this.speedKmh = speedKmh;
        this.overheadMin = overheadMin;
    }

    /** Door-to-door minutes for a straight-line distance: road time plus fixed overhead, at least 1. */
    public int minutesFor(double straightKm) {
        int moving = (int) Math.ceil(straightKm * ROAD_FACTOR / speedKmh * 60);
        return Math.max(1, moving + overheadMin);
    }

    /** Distance-based default for a "Suggested" leg: walk when close, cycle mid-range, else transit. */
    public static TravelMode suggestFor(double straightKm) {
        if (straightKm < WALK_BELOW_KM) {
            return WALK;
        }
        return straightKm < CYCLE_BELOW_KM ? CYCLE : TRANSIT;
    }
}
