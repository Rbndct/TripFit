package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.ExchangeRates;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProposalRulesTest {
    @Test public void dailyRecommendationsCannotBorrowFromAnotherDay() {
        DraftTrip trip=new DraftTrip(); trip.days=3; trip.dailyBudget=true; trip.budget=100; trip.foodBudget=50;
        DraftTrip.Stop stop=new DraftTrip.Stop(); stop.category="Food"; stop.currency="PHP"; stop.cost=45; stop.day=1; trip.stops.add(stop);
        assertEquals(5,DraftBudget.suggestionAllowance(trip,ExchangeRates.samples(),"Food",1),.001);
        assertEquals(50,DraftBudget.suggestionAllowance(trip,ExchangeRates.samples(),"Food",2),.001);
    }
    @Test public void recommendationsRespectTheWholeTripLimitAsWellAsCategoryLimit() {
        DraftTrip trip=new DraftTrip(); trip.budget=100; trip.foodBudget=80;
        DraftTrip.Expense expense=new DraftTrip.Expense(); expense.category="Other"; expense.amount=90; trip.expenses.add(expense);
        assertEquals(10,DraftBudget.suggestionAllowance(trip,ExchangeRates.samples(),"Food",1),.001);
    }
    @Test public void expensesRetainTheirDayAndAreNotAddedToPlannedCostsTwice() {
        DraftTrip trip=new DraftTrip(); trip.days=2; trip.dailyBudget=true; trip.budget=100; trip.foodBudget=50;
        DraftTrip.Expense expense=new DraftTrip.Expense(); expense.day=2; expense.amount=40; trip.expenses.add(expense);
        assertEquals(50,DraftBudget.suggestionAllowance(trip,ExchangeRates.samples(),"Food",1),.001);
        assertEquals(10,DraftBudget.suggestionAllowance(trip,ExchangeRates.samples(),"Food",2),.001);
    }
    @Test public void balancingAndJournalFollowTheStopWithoutMovingABooking() {
        DraftTrip trip=new DraftTrip(); trip.days=2;
        DraftTrip.Stop booked=new DraftTrip.Stop(); booked.booked=true; booked.day=2; booked.fixedMinute=975; trip.stops.add(booked);
        DraftTrip.Stop flexible=new DraftTrip.Stop(); flexible.day=2; trip.stops.add(flexible);
        DraftTrip.Entry entry=new DraftTrip.Entry(); entry.day=2; entry.stopId=flexible.id; trip.journal.add(entry);
        DraftRules.balanceDays(trip);
        assertEquals(2,booked.day); assertEquals(975,booked.fixedMinute);
        assertEquals(flexible.day,DraftRules.journalDay(trip,entry));
    }
    @Test public void manualBlankDurationUsesThePlaceCategory() {
        assertEquals(60,DraftRules.defaultDuration("Activities")); assertEquals(60,DraftRules.defaultDuration("Food"));
        assertEquals(30,DraftRules.defaultDuration("Transport")); assertEquals(30,DraftRules.defaultDuration("Other"));
    }
}
