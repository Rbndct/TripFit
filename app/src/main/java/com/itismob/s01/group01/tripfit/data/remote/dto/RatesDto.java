package com.itismob.s01.group01.tripfit.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Frankfurter's {@code /latest} response: {@code rates} are units of each currency per 1 unit of
 * {@code base} (the base itself is not listed). Gson fills these by reflection, so the fields
 * aren't final; {@link SerializedName} keeps the JSON keys stable if the build is ever minified.
 */
public class RatesDto {

    @SerializedName("amount")
    private double amount;
    @SerializedName("base")
    private String base;
    @SerializedName("date")
    private String date;
    @SerializedName("rates")
    private Map<String, Double> rates;

    public RatesDto() { }

    public double getAmount() {
        return amount;
    }

    public String getBase() {
        return base;
    }

    /** ISO date (yyyy-MM-dd) of the ECB reference rates. */
    public String getDate() {
        return date;
    }

    public Map<String, Double> getRates() {
        return rates;
    }
}
