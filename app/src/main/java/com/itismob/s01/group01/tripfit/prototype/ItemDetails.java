package com.itismob.s01.group01.tripfit.prototype;

import com.google.gson.annotations.SerializedName;

/** Per-itinerary-item extras the Room schema has no column for: price, currency, category, outdoor. */
public final class ItemDetails {

    @SerializedName("cost")
    private double cost;
    @SerializedName("costCurrency")
    private String costCurrency;
    @SerializedName("category")
    private String category;
    @SerializedName("outdoor")
    private boolean outdoor;

    /** For Gson. */
    public ItemDetails() { }

    public ItemDetails(double cost, String costCurrency, String category, boolean outdoor) {
        this.cost = cost;
        this.costCurrency = costCurrency;
        this.category = category;
        this.outdoor = outdoor;
    }

    /**
     * Copy of {@code previous} (or fresh defaults, outdoor on) with a new cost, currency and
     * category. The outdoor flag is kept, and {@code previous} is never changed.
     */
    public static ItemDetails withCost(ItemDetails previous, double cost, String costCurrency,
                                       String category) {
        boolean outdoor = previous == null || previous.outdoor;
        return new ItemDetails(cost, costCurrency, category, outdoor);
    }

    public double getCost() {
        return cost;
    }

    public String getCostCurrency() {
        return costCurrency;
    }

    public String getCategory() {
        return category;
    }

    public boolean isOutdoor() {
        return outdoor;
    }
}
