package com.itismob.s01.group01.tripfit.data.frontend;

/** API-neutral city metadata; currencies, map centers and calendar zones are data, not UI rules. */
public final class Destination {
    private static final java.util.Set<String> ZONES=new java.util.HashSet<>(java.util.Arrays.asList(java.util.TimeZone.getAvailableIDs()));
    public final String id, name, currency, timeZone;
    public final double lat, lng;
    public Destination(String id,String name,String currency,String timeZone,double lat,double lng) {
        if(id==null || id.isEmpty() || name==null || name.isEmpty() || currency==null || !currency.matches("[A-Z]{3}")
                || !ZONES.contains(timeZone) || !Double.isFinite(lat) || !Double.isFinite(lng) || lat< -90 || lat>90 || lng< -180 || lng>180)
            throw new IllegalArgumentException("Invalid destination metadata");
        this.id=id; this.name=name; this.currency=currency; this.timeZone=timeZone;
        this.lat=lat; this.lng=lng;
    }
}
