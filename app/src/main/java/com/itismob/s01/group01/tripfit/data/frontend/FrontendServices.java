package com.itismob.s01.group01.tripfit.data.frontend;

/** Single injection point for the frontend's cache and travel-data adapters. */
public final class FrontendServices {
    public final DraftRepository drafts;
    public final TravelDataSource travel;
    public FrontendServices(DraftRepository drafts,TravelDataSource travel) {
        if(drafts==null || travel==null) throw new IllegalArgumentException("Both frontend adapters are required");
        this.drafts=drafts; this.travel=travel;
    }
}
