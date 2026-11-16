package com.itismob.s01.group01.tripfit.prototype;

/**
 * Trip details the New Trip form collects beyond the persisted name (city, dates, length). The
 * ISO start date ({@code yyyy-MM-dd}, empty when unknown) is the machine-readable form of the
 * {@code dates} display label.
 */
public final class TripMeta {

    private final String city;
    private final String dates;
    private final int days;
    private final String homeCurrency;
    private String startDateIso;

    public TripMeta(String city, String dates, int days, String homeCurrency) {
        this(city, dates, days, homeCurrency, "");
    }

    public TripMeta(String city, String dates, int days, String homeCurrency, String startDateIso) {
        this.city = city;
        this.dates = dates;
        this.days = days;
        this.homeCurrency = homeCurrency;
        this.startDateIso = startDateIso == null ? "" : startDateIso;
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

    public String getStartDateIso() {
        return startDateIso;
    }

    public void setStartDateIso(String startDateIso) {
        this.startDateIso = startDateIso == null ? "" : startDateIso;
    }
}
