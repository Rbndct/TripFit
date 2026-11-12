package com.itismob.s01.group01.tripfit.prototype;

/** A logged expense: the amount as entered (local currency) plus its home-currency conversion. */
public final class Expense {

    private final String title;
    private final Category category;
    private final double amountLocal;
    private final String localCurrency;
    private final double amountHome;

    public Expense(String title, Category category, double amountLocal, String localCurrency,
                   double amountHome) {
        this.title = title;
        this.category = category;
        this.amountLocal = amountLocal;
        this.localCurrency = localCurrency;
        this.amountHome = amountHome;
    }

    public String getTitle() {
        return title;
    }

    public Category getCategory() {
        return category;
    }

    public double getAmountLocal() {
        return amountLocal;
    }

    public String getLocalCurrency() {
        return localCurrency;
    }

    public double getAmountHome() {
        return amountHome;
    }
}
