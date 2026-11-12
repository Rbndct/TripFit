package com.itismob.s01.group01.tripfit.prototype;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ScheduleMathTest {

    @Test
    public void build_flexibleStops_flowWithBufferFromNineAm() {
        List<ScheduleMath.Slot> slots = ScheduleMath.build(Arrays.asList(
                new ScheduleMath.Stop("A", null, 45),
                new ScheduleMath.Stop("B", null, 30)), 15);
        assertEquals(9 * 60, slots.get(0).getStartMin());
        assertEquals(9 * 60 + 45, slots.get(0).getEndMin());
        assertEquals(9 * 60 + 60, slots.get(1).getStartMin());
    }

    @Test
    public void build_missingDuration_usesDefaultStay() {
        List<ScheduleMath.Slot> slots = ScheduleMath.build(
                java.util.Collections.singletonList(new ScheduleMath.Stop("A", null, null)), 0);
        assertEquals(ScheduleMath.DEFAULT_STAY_MIN, slots.get(0).getEndMin() - slots.get(0).getStartMin());
    }

    @Test
    public void build_bookedStop_isPinnedToItsFixedTime() {
        List<ScheduleMath.Slot> slots = ScheduleMath.build(Arrays.asList(
                new ScheduleMath.Stop("A", null, 30),
                new ScheduleMath.Stop("Dinner", "18:00", 90)), 15);
        assertEquals(18 * 60, slots.get(1).getStartMin());
        assertEquals(19 * 60 + 30, slots.get(1).getEndMin());
    }

    @Test
    public void format_padsHoursAndMinutes() {
        assertEquals("09:05", ScheduleMath.format(9 * 60 + 5));
        assertEquals("00:30", ScheduleMath.format(24 * 60 + 30));
    }

    @Test
    public void parse_roundTripsWithFormat() {
        assertEquals(15 * 60 + 30, ScheduleMath.parse("15:30"));
    }
}
