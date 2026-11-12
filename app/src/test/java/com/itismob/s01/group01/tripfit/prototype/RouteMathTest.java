package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RouteMathTest {

    // Four points on a line (longitude only): 0, 3, 1, 2 -> optimal order from index 0 is 0,2,3,1
    private static final double[][] LINE = {{0, 0}, {0, 3}, {0, 1}, {0, 2}};

    @Test
    public void optimize_unlocked_visitsNearestNeighbourFirst() {
        int[] order = RouteMath.optimize(LINE, new boolean[4]);
        assertArrayEquals(new int[]{0, 2, 3, 1}, order);
    }

    @Test
    public void optimize_reducesTotalDistance() {
        double before = RouteMath.totalKm(LINE, RouteMath.identity(4));
        double after = RouteMath.totalKm(LINE, RouteMath.optimize(LINE, new boolean[4]));
        assertTrue(after < before);
    }

    @Test
    public void optimize_lockedStop_keepsItsSlot() {
        boolean[] locked = {false, false, true, false};
        int[] order = RouteMath.optimize(LINE, locked);
        assertEquals(2, order[2]);
    }

    @Test
    public void optimize_resultIsAPermutation() {
        int[] order = RouteMath.optimize(LINE, new boolean[]{false, true, false, false});
        boolean[] seen = new boolean[4];
        for (int index : order) {
            seen[index] = true;
        }
        for (boolean s : seen) {
            assertTrue(s);
        }
    }

    @Test
    public void optimize_singleStop_returnsIdentity() {
        assertArrayEquals(new int[]{0}, RouteMath.optimize(new double[][]{{1, 1}}, new boolean[1]));
    }
}
