package com.itismob.s01.group01.tripfit.prototype;

/** A demo suggestion (restaurant, attraction, or Discover spot) with its price in local currency. */
public final class MockPlace {

    private final String name;
    private final String kind;
    private final double rating;
    private final int reviews;
    private final double distanceKm;
    private final double priceLocal;
    private final boolean indoor;

    public MockPlace(String name, String kind, double rating, int reviews, double distanceKm,
                     double priceLocal, boolean indoor) {
        this.name = name;
        this.kind = kind;
        this.rating = rating;
        this.reviews = reviews;
        this.distanceKm = distanceKm;
        this.priceLocal = priceLocal;
        this.indoor = indoor;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public double getRating() {
        return rating;
    }

    public int getReviews() {
        return reviews;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getPriceLocal() {
        return priceLocal;
    }

    public boolean isIndoor() {
        return indoor;
    }
}
