package com.itismob.s01.group01.tripfit.prototype;

/** Trip details the New Trip form collects beyond the persisted name (city, dates, length). */
public final class TripMeta {

    private final String city;
    private final String dates;
    private final int days;
    private final String homeCurrency;

    public TripMeta(String city, String dates, int days, String homeCurrency) {
        this.city = city;
        this.dates = dates;
        this.days = days;
        this.homeCurrency = homeCurrency;
    }

    public String getCity() {
        return city;
    }

    public String getDates() {
        return dates;
    }

    public int getDays() {
        return days;
    }

    public String getHomeCurrency() {
        return homeCurrency;
    }
}
