package com.itismob.s01.group01.tripfit.prototype;

/** Nearest-neighbour route ordering that never moves booked (locked) stops. */
public final class RouteMath {

    private static final double KM_PER_DEG_LAT = 111.0;
    private static final double KM_PER_DEG_LNG = 91.0;

    private RouteMath() { }

    /** Flat-earth distance in km between two {lat, lng} points; fine at city scale. */
    public static double km(double[] a, double[] b) {
        return Math.hypot((a[0] - b[0]) * KM_PER_DEG_LAT, (a[1] - b[1]) * KM_PER_DEG_LNG);
    }

    public static double totalKm(double[][] points, int[] order) {
        double total = 0;
        for (int i = 1; i < order.length; i++) {
            total += km(points[order[i - 1]], points[order[i]]);
        }
        return total;
    }

    /**
     * Returns the visiting order as indices into {@code points}. Locked stops keep their slot;
     * each free slot takes the nearest unused free stop to the stop before it.
     */
    public static int[] optimize(double[][] points, boolean[] locked) {
        int n = points.length;
        int[] order = new int[n];
        boolean[] used = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (locked[i]) {
                order[i] = i;
                used[i] = true;
            }
        }
        for (int slot = 0; slot < n; slot++) {
            if (locked[slot]) {
                continue;
            }
            int best = nearestUnused(points, used, slot == 0 ? -1 : order[slot - 1]);
            order[slot] = best;
            used[best] = true;
        }
        return order;
    }

    private static int nearestUnused(double[][] points, boolean[] used, int from) {
        int best = -1;
        double bestKm = Double.MAX_VALUE;
        for (int j = 0; j < points.length; j++) {
            if (used[j]) {
                continue;
            }
            double distance = from < 0 ? j : km(points[from], points[j]);
            if (distance < bestKm) {
                bestKm = distance;
                best = j;
            }
        }
        return best;
    }

    public static int[] identity(int n) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        return order;
    }
}
