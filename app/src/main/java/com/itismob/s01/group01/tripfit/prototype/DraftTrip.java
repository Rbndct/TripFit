package com.itismob.s01.group01.tripfit.prototype;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local prototype state. No account, network service, or API key is required. */
public class DraftTrip {
    public String id = UUID.randomUUID().toString();
    public String name = "";
    public String city = "Tokyo";
    public String startDate = "2026-12-01";
    public int days = 3;
    public String homeCurrency = "PHP";
    public double budget = 15000;
    public boolean dailyBudget;
    public double foodBudget = 4500, activityBudget = 6000, transportBudget = 3000, otherBudget = 1500;
    public boolean sample;
    public boolean calendarPreview;
    public List<Stop> stops = new ArrayList<>();
    public List<Expense> expenses = new ArrayList<>();
    public List<String> members = new ArrayList<>();
    public List<String> votes = new ArrayList<>();
    public List<Entry> journal = new ArrayList<>();

    public String localCurrency() { return city.equals("Tokyo") ? "JPY" : "PHP"; }
    public double totalBudget() { return budget * (dailyBudget ? days : 1); }
    public double categoryBudget(String category) {
        double value = category.equals("Food") ? foodBudget : category.equals("Activities") ? activityBudget
                : category.equals("Transport") ? transportBudget : otherBudget;
        return value * (dailyBudget ? days : 1);
    }

    public static class Stop {
        public String id = UUID.randomUUID().toString();
        public String name = "", address = "", category = "Activities", mode = "Suggested";
        public int day = 1, duration = 60, buffer = 15, fixedMinute = 540;
        public double lat, lng, cost;
        public boolean hasLocation, booked, outdoor = true;
    }
    public static class Expense {
        public String id = UUID.randomUUID().toString();
        public String title = "", category = "Food", currency = "PHP";
        public double amount;
    }
    public static class Entry {
        public String id = UUID.randomUUID().toString();
        public int day = 1;
        public String stopId = "", note = "", photo = "";
    }
}
