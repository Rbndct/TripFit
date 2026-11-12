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
    public void suggestFor_midDistance_isTransit() {
        assertEquals(TravelMode.TRANSIT, TravelMode.suggestFor(3.0));
    }

    @Test
    public void suggestFor_longDistance_isDrive() {
        assertEquals(TravelMode.DRIVE, TravelMode.suggestFor(12.0));
    }

    @Test
    public void minutesFor_walkingOneKm_isTwelveMinutes() {
        assertEquals(12, TravelMode.WALK.minutesFor(1.0));
    }

    @Test
    public void minutesFor_drivingIsFasterThanWalkingOverFiveKm() {
        assertTrue(TravelMode.DRIVE.minutesFor(5) < TravelMode.WALK.minutesFor(5));
    }
}
