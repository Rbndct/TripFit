package com.itismob.s01.group01.tripfit.ui;

import android.content.Context;

import com.itismob.s01.group01.tripfit.R;
import com.itismob.s01.group01.tripfit.data.CurrencyRates;
import com.itismob.s01.group01.tripfit.data.local.ItineraryItemWithPlace;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.CatalogSuggestions;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.DemoCatalog;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.prototype.TripMeta;
import com.itismob.s01.group01.tripfit.prototype.TripSnapshots;

import java.util.List;
import java.util.Map;

/** Formats shared catalog cards using one budget snapshot per render. */
public final class CatalogRows {
    private final Context context;
    private final BudgetSettings budget;
    private final int days;
    private final Map<Category, Double> planned;
    private final Map<Category, Double> logged;

    public CatalogRows(Context context, String tripId, PrototypeStore store,
                       List<ItineraryItemWithPlace> itinerary) {
        this.context = context;
        TripMeta meta = store.meta(tripId);
        days = meta.getDays();
        budget = store.budget(tripId);
        planned = BudgetMath.plannedByCategory(TripSnapshots.from(tripId, meta, itinerary, store));
        logged = BudgetMath.spentByCategory(store.expenses(tripId));
    }

    private double remaining(Category category) {
        return CatalogSuggestions.remaining(budget.limitFor(category, days),
                planned.get(category), logged.get(category));
    }

    /** Tighter of the category and whole-trip remaining; unbounded when no limits are set. */
    private double allowance(Category category) {
        return CatalogSuggestions.allowance(budget.limitFor(category, days),
                planned.get(category), logged.get(category),
                budget.tripTotal(days), sum(planned), sum(logged));
    }

    private static double sum(Map<Category, Double> amounts) {
        double total = 0;
        for (double amount : amounts.values()) {
            total += amount;
        }
        return total;
    }

    private double homePrice(DemoCatalog.Place place) {
        return CurrencyRates.convert(place.price, place.currency, budget.getHomeCurrency());
    }

    public boolean isOverBudget(DemoCatalog.Place place) {
        Category category = BudgetMath.parseCategory(place.category);
        return homePrice(place) > allowance(category);
    }

    public String nearbyLabel(CatalogSuggestions.Anchor anchor) {
        return context.getString(R.string.catalog_nearby, anchor.name == null
                ? context.getString(R.string.catalog_city_centre) : anchor.name);
    }

    public String subtitle(DemoCatalog.Place place, double distance, boolean alreadyPlanned) {
        Category category = BudgetMath.parseCategory(place.category);
        String categoryLabel = Labels.category(context, category);
        String tier = context.getString(place.priceTier() == 0 ? R.string.catalog_free
                : place.priceTier() == 1 ? R.string.catalog_price_low : R.string.catalog_price_high);
        String text = context.getString(R.string.catalog_card,
                context.getString(place.hidden ? R.string.catalog_hidden : R.string.catalog_popular),
                categoryLabel,
                context.getString(place.outdoor ? R.string.catalog_outdoors : R.string.catalog_indoors),
                place.description, place.rating,
                context.getResources().getQuantityString(
                        R.plurals.catalog_reviews, place.reviews, place.reviews),
                distance, tier,
                CurrencyRates.format(place.price, place.currency),
                CurrencyRates.format(homePrice(place), budget.getHomeCurrency()), place.minutes);
        if (budget.limitFor(category, days) > 0) {
            text += "\n" + context.getString(R.string.catalog_remaining,
                    CurrencyRates.format(remaining(category), budget.getHomeCurrency()), categoryLabel);
            if (isOverBudget(place)) {
                text += " · " + context.getString(R.string.catalog_over_budget);
            }
        } else {
            text += "\n" + context.getString(R.string.catalog_no_limit);
        }
        if (alreadyPlanned) {
            text += "\n" + context.getString(R.string.catalog_already_planned);
        }
        return text;
    }
}
