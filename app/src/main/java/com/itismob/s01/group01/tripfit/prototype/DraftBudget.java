package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.ExchangeRates;

/** Shared budget rules for discovery, the tracker, group splits, and exports. */
public final class DraftBudget {
    public static double planned(DraftTrip trip,ExchangeRates rates,String category,Integer day) {
        double total=0;
        for(DraftTrip.Stop stop:trip.stops) if((category==null || stop.category.equals(category)) && (day==null || stop.day==day)) total+=trip.convertedCost(stop,rates);
        return total;
    }
    public static double spent(DraftTrip trip,ExchangeRates rates,String category,Integer day) {
        double total=0;
        for(DraftTrip.Expense expense:trip.expenses) if((category==null || expense.category.equals(category)) && (day==null || expense.day==day)) total+=rates.convert(expense.amount,expense.currency,trip.homeCurrency);
        return total;
    }
    public static double suggestionAllowance(DraftTrip trip,ExchangeRates rates,String category,int day) {
        double categoryRemaining=trip.categoryBudget(category)-Math.max(planned(trip,rates,category,null),spent(trip,rates,category,null));
        double tripRemaining=trip.totalBudget()-Math.max(planned(trip,rates,null,null),spent(trip,rates,null,null));
        double remaining=Math.min(categoryRemaining,tripRemaining);
        if(trip.dailyBudget) {
            double dailyCategory=trip.categoryBudget(category)/trip.days-Math.max(planned(trip,rates,category,day),spent(trip,rates,category,day));
            double dailyOverall=trip.budget-Math.max(planned(trip,rates,null,day),spent(trip,rates,null,day));
            remaining=Math.min(remaining,Math.min(dailyCategory,dailyOverall));
        }
        return remaining;
    }
    private DraftBudget() {}
}
