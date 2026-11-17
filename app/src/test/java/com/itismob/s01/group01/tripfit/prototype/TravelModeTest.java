package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TravelModeTest {

    @Test
    public void suggestFor_shortDistance_isWalk() {
        assertEquals(TravelMode.WALK, TravelMode.suggestFor(0.8));
    }

    @Test
    public void suggestFor_midDistance_isCycle() {
        assertEquals(TravelMode.CYCLE, TravelMode.suggestFor(3.0));
    }

    @Test
    public void suggestFor_longDistance_isTransit() {
        assertEquals(TravelMode.TRANSIT, TravelMode.suggestFor(12.0));
        assertEquals(TravelMode.TRANSIT, TravelMode.suggestFor(5.0));
    }

    @Test
    public void suggestFor_boundaries_roundUpToTheSlowerBand() {
        assertEquals(TravelMode.CYCLE, TravelMode.suggestFor(1.2));
        assertEquals(TravelMode.WALK, TravelMode.suggestFor(1.19));
        assertEquals(TravelMode.TRANSIT, TravelMode.suggestFor(4.0));
        assertEquals(TravelMode.CYCLE, TravelMode.suggestFor(3.99));
    }

    @Test
    public void minutesFor_walkingOneKm_includesRoadFactor() {
        assertEquals(17, TravelMode.WALK.minutesFor(1.0));
    }

    @Test
    public void minutesFor_transitAddsEightMinuteOverhead() {
        // 10 km * 1.25 / 18 km/h = 41.67 min -> 42, plus 8
        assertEquals(50, TravelMode.TRANSIT.minutesFor(10.0));
    }

    @Test
    public void minutesFor_zeroDistance_isAtLeastOneMinute() {
        assertEquals(1, TravelMode.WALK.minutesFor(0.0));
        assertEquals(1, TravelMode.DRIVE.minutesFor(0.0));
    }

    @Test
    public void minutesFor_drivingIsFasterThanWalkingOverFiveKm() {
        assertTrue(TravelMode.DRIVE.minutesFor(5) < TravelMode.WALK.minutesFor(5));
    }
}
