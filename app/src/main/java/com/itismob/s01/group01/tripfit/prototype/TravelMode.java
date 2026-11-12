package com.itismob.s01.group01.tripfit.prototype;

/** Per-leg transport modes with demo speeds (OpenRouteService/Google Directions stand-ins). */
public enum TravelMode {
    WALK(5.0, 0),
    CYCLE(15.0, 2),
    DRIVE(30.0, 5),
    TRANSIT(22.0, 8);

    private final double speedKmh;
    private final int overheadMin;

    TravelMode(double speedKmh, int overheadMin) {
        this.speedKmh = speedKmh;
        this.overheadMin = overheadMin;
    }

    /** Door-to-door minutes for a leg: moving time plus fixed parking/waiting overhead. */
    public int minutesFor(double km) {
        return (int) Math.round(km / speedKmh * 60) + overheadMin;
    }

    /** Distance-based default: walk when close, transit mid-range, drive when far. */
    public static TravelMode suggestFor(double km) {
        if (km < 1.5) {
            return WALK;
        }
        return km < 6 ? TRANSIT : DRIVE;
    }
}
