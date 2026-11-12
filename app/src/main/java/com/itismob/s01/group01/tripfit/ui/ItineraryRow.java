package com.itismob.s01.group01.tripfit.ui;

import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

/** A stop row within the active day's list (day grouping is now handled by the day tab strip). */
public final class ItineraryRow {

    private final ItineraryItemEntity item;
    private final String placeName;
    private final int indexInDay;

    public ItineraryRow(ItineraryItemEntity item, String placeName, int indexInDay) {
        this.item = item;
        this.placeName = placeName;
        this.indexInDay = indexInDay;
    }

    public ItineraryItemEntity getItem() {
        return item;
    }

    public String getPlaceName() {
        return placeName;
    }

    public int getIndexInDay() {
        return indexInDay;
    }
}
