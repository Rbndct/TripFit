package com.itismob.s01.group01.tripfit.data.local;

import androidx.room.Embedded;

import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;

/** Room projection: an itinerary item left-joined with its referenced place's name, if any. */
public class ItineraryItemWithPlace {

    @Embedded
    public ItineraryItemEntity item;
    public String placeName;
}
