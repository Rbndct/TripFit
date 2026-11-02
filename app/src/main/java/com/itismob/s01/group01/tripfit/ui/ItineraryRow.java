package com.itismob.s01.group01.tripfit.ui;

import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

/** A row in the itinerary list: either a day header or a stop. */
public abstract class ItineraryRow {

    public static final int TYPE_DAY_HEADER = 0;
    public static final int TYPE_STOP = 1;

    public abstract int getViewType();

    public static final class DayHeader extends ItineraryRow {
        private final int day;
        private final int stopCount;

        public DayHeader(int day, int stopCount) {
            this.day = day;
            this.stopCount = stopCount;
        }

        public int getDay() {
            return day;
        }

        public int getStopCount() {
            return stopCount;
        }

        @Override
        public int getViewType() {
            return TYPE_DAY_HEADER;
        }
    }

    public static final class Stop extends ItineraryRow {
        private final ItineraryItemEntity item;
        private final int indexInDay;

        public Stop(ItineraryItemEntity item, int indexInDay) {
            this.item = item;
            this.indexInDay = indexInDay;
        }

        public ItineraryItemEntity getItem() {
            return item;
        }

        public int getIndexInDay() {
            return indexInDay;
        }

        @Override
        public int getViewType() {
            return TYPE_STOP;
        }
    }
}
