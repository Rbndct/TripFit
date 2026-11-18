package com.itismob.s01.group01.tripfit.data.local;

import androidx.room.Embedded;

import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

/** Room projection: an itinerary item left-joined with its referenced place's name, address and pin, if any. */
public class ItineraryItemWithPlace {

    @Embedded
    public ItineraryItemEntity item;
    public String placeName;
    public String placeAddress;
    /** Pin of the referenced place; null when the stop has no place (no coordinates). */
    public Double placeLat, placeLng;
}
