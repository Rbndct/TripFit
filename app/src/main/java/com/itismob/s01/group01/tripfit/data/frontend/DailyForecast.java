package com.itismob.s01.group01.tripfit.data.frontend;

/** Vendor-neutral forecast, indexed by the trip's day in the cached travel snapshot. */
public final class DailyForecast {
    public final String condition, source;
    public final Integer temperatureC, rainPercent;
    public final boolean sample;
    public DailyForecast(String condition,Integer temperatureC,Integer rainPercent,String source,boolean sample) {
        this.condition=condition; this.temperatureC=temperatureC; this.rainPercent=rainPercent;
        this.source=source; this.sample=sample;
    }
    public boolean risky() { return condition.equals("Rain") || condition.equals("Storm"); }
    public String display() {
        if(temperatureC==null) return "Forecast unavailable · Refresh travel information before planning around weather.";
        return condition+" · "+temperatureC+"°C · "+rainPercent+"% rain chance · "+source;
    }
}
