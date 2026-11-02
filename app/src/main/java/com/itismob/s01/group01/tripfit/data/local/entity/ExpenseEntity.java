package com.itismob.s01.group01.tripfit.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Mirrors the per-user `expenses` table in Supabase (API-Integration-Plan.md §3). */
@Entity(
        tableName = "expenses",
        foreignKeys = @ForeignKey(
                entity = TripEntity.class,
                parentColumns = "id",
                childColumns = "tripId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("tripId")}
)
public class ExpenseEntity {

    @PrimaryKey
    @NonNull
    private final String id;
    private final String tripId;
    private final String itineraryItemId;
    private final double amount;
    private final String currency;
    private final Double convertedAmount;
    private final String category;

    public ExpenseEntity(@NonNull String id, String tripId, String itineraryItemId, double amount,
                          String currency, Double convertedAmount, String category) {
        this.id = id;
        this.tripId = tripId;
        this.itineraryItemId = itineraryItemId;
        this.amount = amount;
        this.currency = currency;
        this.convertedAmount = convertedAmount;
        this.category = category;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getTripId() {
        return tripId;
    }

    public String getItineraryItemId() {
        return itineraryItemId;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Double getConvertedAmount() {
        return convertedAmount;
    }

    public String getCategory() {
        return category;
    }
}
