package com.itismob.s01.group01.tripfit.prototype;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class BudgetActivity extends PrototypeActivity {
    private static final String[] CATEGORIES={"Food","Activities","Transport","Other"};
    private static final String[] CURRENCIES={"PHP","JPY","USD","EUR"};
    private static final int DECIMAL=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL;
    @Override protected void onCreate(Bundle state) { super.onCreate(state); if(trip==null) { finish(); return; } render(); }
    private double planned(String category) {
        double total=0; for(DraftTrip.Stop stop:trip.stops) if(category==null || stop.category.equals(category)) total+=trip.convertedCost(stop); return total;
    }
    private double actual(String category) {
        double total=0; for(DraftTrip.Expense expense:trip.expenses) if(category==null || expense.category.equals(category)) total+=RoutePreview.convert(expense.amount,expense.currency,trip.homeCurrency); return total;
    }
    private void render() {
        reload(); page("A little budget clarity",trip.name+" · "+trip.homeCurrency,true);
        notice("Sample exchange rates · 1 JPY = 0.38 PHP, 1 USD = 58 PHP, 1 EUR = 63 PHP. Estimates are illustrative and exclude unpriced travel legs. Planned costs and logged expenses are shown separately.");
        LinearLayout summary=card(content);
        summary.addView(text(money(trip.totalBudget()-actual(null),trip.homeCurrency),26,actual(null)>trip.totalBudget()?0xffa33232:TEAL));
        label(summary,"Remaining after logged expenses · "+money(trip.totalBudget(),trip.homeCurrency)+" trip limit");
        label(summary,"Planned: "+money(planned(null),trip.homeCurrency)+" / Logged: "+money(actual(null),trip.homeCurrency));
        if(planned(null)>trip.totalBudget()) label(summary,"Your planned itinerary is over budget. Review paid stops or choose affordable suggestions.");
        for(String category:CATEGORIES) {
            LinearLayout card=card(content); double used=actual(category), limit=trip.categoryBudget(category);
            card.addView(text(category,18,INK)); label(card,"Planned "+money(planned(category),trip.homeCurrency)+" · Logged "+money(used,trip.homeCurrency));
            ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); progress.setProgress(limit<=0?0:(int)Math.min(100,used/limit*100)); card.addView(progress);
            label(card,money(limit-used,trip.homeCurrency)+" remaining of "+money(limit,trip.homeCurrency)+(used>limit?" · Over budget":""));
        }
        title("Set your limits");
        TextInputEditText total=field(content,"Budget ("+trip.homeCurrency+")",plainAmount(trip.budget),DECIMAL);
        CheckBox daily=check(content,"Daily budget and daily category limits",trip.dailyBudget);
        TextInputEditText[] categories=new TextInputEditText[4];
        double[] limits={trip.foodBudget,trip.activityBudget,trip.transportBudget,trip.otherBudget};
        for(int i=0;i<4;i++) categories[i]=field(content,CATEGORIES[i]+" limit",plainAmount(limits[i]),DECIMAL);
        button(content,"Save budget limits",()->{
            try {
                double amount=number(total,1,100000000), sum=0; double[] values=new double[4];
                for(int i=0;i<4;i++) { values[i]=number(categories[i],0,100000000); sum+=values[i]; }
                if(sum>amount+.01) { total.setError("Category limits cannot exceed the overall limit"); return; }
                trip.budget=amount; trip.dailyBudget=daily.isChecked(); trip.foodBudget=values[0]; trip.activityBudget=values[1]; trip.transportBudget=values[2]; trip.otherBudget=values[3]; save(); toast("Budget saved"); render();
            } catch(IllegalArgumentException ignored) {}
        });
        title("Log an expense");
        TextInputEditText title=field(content,"What did you spend on?","",InputType.TYPE_CLASS_TEXT);
        TextInputEditText amount=field(content,"Amount","",DECIMAL);
        Spinner category=choices(content,"Expense category",CATEGORIES,"Food");
        Spinner currency=choices(content,"Paid in",CURRENCIES,trip.localCurrency());
        button(content,"Add expense",()->{
            if(value(title).isEmpty()) { title.setError("Enter an expense description"); return; }
            try { DraftTrip.Expense expense=new DraftTrip.Expense(); expense.title=value(title); expense.amount=number(amount,.01,100000000); expense.category=category.getSelectedItem().toString(); expense.currency=currency.getSelectedItem().toString(); trip.expenses.add(expense); save(); render(); }
            catch(IllegalArgumentException ignored) {}
        });
        title("Expense history");
        if(trip.expenses.isEmpty()) label(content,"No expenses yet. Log one to see your remaining budget update.");
        for(DraftTrip.Expense expense:trip.expenses) {
            LinearLayout card=card(content); card.addView(text(expense.title,18,INK));
            label(card,expense.category+" · "+money(expense.amount,expense.currency)+" ≈ "+money(RoutePreview.convert(expense.amount,expense.currency,trip.homeCurrency),trip.homeCurrency));
            button(card,"Remove expense",()->new MaterialAlertDialogBuilder(this).setTitle("Remove this expense?").setNegativeButton("Keep",null).setPositiveButton("Remove",(d,w)->{trip.expenses.remove(expense);save();render();}).show());
        }
        title("Currency calculator");
        TextInputEditText input=field(content,"Amount to convert","1000",DECIMAL);
        Spinner from=choices(content,"From",CURRENCIES,trip.localCurrency());
        TextView converted=text("",20,TEAL); content.addView(converted);
        button(content,"Convert to "+trip.homeCurrency,()->{
            try { double value=number(input,0,100000000); converted.setText(money(RoutePreview.convert(value,from.getSelectedItem().toString(),trip.homeCurrency),trip.homeCurrency)+" · sample rate"); }
            catch(IllegalArgumentException ignored) {}
        });
        button(content,"Find budget-friendly food & activities",()->openFeature("DiscoverActivity"));
    }
}
