package com.itismob.s01.group01.tripfit.prototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lays a day's stops onto a clock: flexible stops flow after the previous one, booked stops are pinned. */
public final class ScheduleMath {

    public static final int DAY_START_MIN = 9 * 60;
    public static final int DEFAULT_STAY_MIN = 60;

    private ScheduleMath() { }

    /** Input for one stop; {@code fixedTime} ("HH:mm") is non-null for booked stops. */
    public static final class Stop {
        private final String name;
        private final String fixedTime;
        private final Integer durationMin;

        public Stop(String name, String fixedTime, Integer durationMin) {
            this.name = name;
            this.fixedTime = fixedTime;
            this.durationMin = durationMin;
        }
    }

    /** A scheduled stop with minute-of-day start/end. */
    public static final class Slot {
        private final String name;
        private final int startMin;
        private final int endMin;

        Slot(String name, int startMin, int endMin) {
            this.name = name;
            this.startMin = startMin;
            this.endMin = endMin;
        }

        public String getName() {
            return name;
        }

        public int getStartMin() {
            return startMin;
        }

        public int getEndMin() {
            return endMin;
        }
    }

    /** Builds the day's schedule, inserting {@code bufferMin} between consecutive stops. */
    public static List<Slot> build(List<Stop> stops, int bufferMin) {
        List<Slot> slots = new ArrayList<>();
        int cursor = DAY_START_MIN;
        for (Stop stop : stops) {
            int start = stop.fixedTime != null ? parse(stop.fixedTime) : cursor;
            int stay = stop.durationMin != null ? stop.durationMin : DEFAULT_STAY_MIN;
            slots.add(new Slot(stop.name, start, start + stay));
            cursor = start + stay + bufferMin;
        }
        return slots;
    }

    public static int parse(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public static String format(int minuteOfDay) {
        int wrapped = minuteOfDay % (24 * 60);
        return String.format(Locale.US, "%02d:%02d", wrapped / 60, wrapped % 60);
    }
}
