package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.prototype.DraftTrip;

/** Cached, mode-specific leg estimates. Populate an API matrix asynchronously before using it here. */
public interface TravelTimeProvider {
    int minutes(DraftTrip.Stop from,DraftTrip.Stop to);
    String mode(DraftTrip.Stop from,DraftTrip.Stop to);
}
