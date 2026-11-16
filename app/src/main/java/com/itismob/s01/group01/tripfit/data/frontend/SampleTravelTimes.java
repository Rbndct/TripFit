package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.prototype.DraftTrip;
import com.itismob.s01.group01.tripfit.prototype.RoutePreview;

/** The prototype estimator; replace this binding with a cached ORS/transit matrix later. */
public final class SampleTravelTimes implements TravelTimeProvider {
    @Override public int minutes(DraftTrip.Stop from,DraftTrip.Stop to) { return RoutePreview.travel(from,to); }
    @Override public String mode(DraftTrip.Stop from,DraftTrip.Stop to) { return RoutePreview.mode(from,to); }
}
