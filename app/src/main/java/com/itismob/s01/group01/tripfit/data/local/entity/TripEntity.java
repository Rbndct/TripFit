package com.itismob.s01.group01.tripfit.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Mirrors the per-user `trips` table in Supabase (API-Integration-Plan.md §3). Local-only
 * until Auth (Phase 5) is wired; {@code userId} stays null until then.
 */
@Entity(tableName = "trips")
public class TripEntity {

    @PrimaryKey
    @NonNull
    private final String id;
    private final String userId;
    private final String name;
    private final String startDate;
    private final String endDate;
    private final String homeCurrency;
    private final Double budget;

    public TripEntity(@NonNull String id, String userId, String name, String startDate,
                       String endDate, String homeCurrency, Double budget) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.homeCurrency = homeCurrency;
        this.budget = budget;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getHomeCurrency() {
        return homeCurrency;
    }

    public Double getBudget() {
        return budget;
    }
}
