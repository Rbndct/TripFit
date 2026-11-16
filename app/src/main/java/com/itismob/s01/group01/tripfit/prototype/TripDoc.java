package com.itismob.s01.group01.tripfit.prototype;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted DTO for one trip (one JSON document per trip id). Plain mutable fields on purpose.
 * A null list/object means "never set": the store then applies its defaults or seed.
 */
final class TripDoc {

    @SerializedName("meta")
    MetaRec meta;
    @SerializedName("budget")
    BudgetRec budget;
    @SerializedName("expenses")
    List<ExpenseRec> expenses;
    @SerializedName("journal")
    List<JournalRec> journal;
    @SerializedName("votes")
    List<String> votes = new ArrayList<>();
    @SerializedName("members")
    List<String> members;
    @SerializedName("weather")
    List<String> weather = new ArrayList<>();
    @SerializedName("synced")
    boolean synced;
    @SerializedName("sample")
    boolean sample;

    TripDoc() { }

    static final class MetaRec {
        @SerializedName("city")
        String city;
        @SerializedName("dates")
        String dates;
        @SerializedName("startDate")
        String startDate;
        @SerializedName("days")
        int days;
        @SerializedName("homeCurrency")
        String homeCurrency;

        MetaRec() { }
    }

    static final class BudgetRec {
        @SerializedName("homeCurrency")
        String homeCurrency;
        @SerializedName("localCurrency")
        String localCurrency;
        @SerializedName("amount")
        double amount;
        @SerializedName("perDay")
        boolean perDay;
        @SerializedName("limits")
        Map<String, Double> limits = new HashMap<>();

        BudgetRec() { }
    }

    static final class ExpenseRec {
        @SerializedName("id")
        String id;
        @SerializedName("title")
        String title;
        @SerializedName("category")
        String category;
        @SerializedName("localCurrency")
        String localCurrency;
        @SerializedName("amountLocal")
        double amountLocal;

        ExpenseRec() { }
    }

    static final class JournalRec {
        @SerializedName("id")
        String id;
        @SerializedName("day")
        int day;
        @SerializedName("stopName")
        String stopName;
        @SerializedName("stopId")
        String stopId;
        @SerializedName("note")
        String note;
        @SerializedName("hasPhoto")
        boolean hasPhoto;
        @SerializedName("photoUri")
        String photoUri;

        JournalRec() { }
    }
}
