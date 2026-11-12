package com.itismob.s01.group01.tripfit;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.itismob.s01.group01.tripfit.data.TripRepository;
import com.itismob.s01.group01.tripfit.prototype.BudgetMath;
import com.itismob.s01.group01.tripfit.prototype.BudgetSettings;
import com.itismob.s01.group01.tripfit.prototype.Category;
import com.itismob.s01.group01.tripfit.prototype.CurrencyMock;
import com.itismob.s01.group01.tripfit.prototype.MockData;
import com.itismob.s01.group01.tripfit.prototype.MockPlace;
import com.itismob.s01.group01.tripfit.prototype.PrototypeStore;
import com.itismob.s01.group01.tripfit.ui.Labels;
import com.itismob.s01.group01.tripfit.ui.RowAdapter;
import com.itismob.s01.group01.tripfit.ui.UiRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Budget-aware nearby food and activities. Prices convert to the home currency, and the
 * "Fits my budget" chip hides anything above the category's remaining budget.
 */
public class SuggestionsActivity extends ScreenActivity {

    public static final String EXTRA_DAY = "extra_day";
    private static final int TAB_FOOD = 0;

    private final PrototypeStore store = PrototypeStore.get();
    private final RowAdapter adapter = new RowAdapter();
    private TripRepository repository;
    private TabLayout tabs;
    private Chip fitsBudget;
    private int targetDay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreen(R.layout.activity_suggestions, R.string.suggestions_title);
        repository = ((TripFitApp) getApplication()).getTripRepository();
        targetDay = getIntent().getIntExtra(EXTRA_DAY, 1);

        ((RecyclerView) findViewById(R.id.list_suggestions)).setAdapter(adapter);
        tabs = findViewById(R.id.tabs);
        fitsBudget = findViewById(R.id.chip_fits_budget);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                render();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
        fitsBudget.setOnCheckedChangeListener((chip, checked) -> render());
        render();
    }

    private void render() {
        boolean food = tabs.getSelectedTabPosition() == TAB_FOOD;
        Category category = food ? Category.FOOD : Category.ACTIVITIES;
        BudgetSettings budget = store.budget(tripId);
        String home = budget.getHomeCurrency();
        String local = budget.getLocalCurrency();
        double limit = budget.limitFor(category);
        double spent = BudgetMath.spentByCategory(store.expenses(tripId)).get(category);
        double remaining = BudgetMath.remaining(limit, spent);

        TextView hint = findViewById(R.id.text_budget_hint);
        hint.setText(limit > 0
                ? getString(R.string.suggestions_budget_hint,
                        CurrencyMock.format(Math.max(0, remaining), home),
                        Labels.category(this, category))
                : "");

        List<UiRow> rows = new ArrayList<>();
        for (MockPlace place : food ? MockData.food() : MockData.activities()) {
            double priceHome = CurrencyMock.convert(place.getPriceLocal(), local, home);
            boolean fits = limit <= 0 || priceHome <= remaining;
            if (fitsBudget.isChecked() && !fits) {
                continue;
            }
            rows.add(toRow(place, priceHome, home, local));
        }
        adapter.submit(rows);
        findViewById(R.id.text_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private UiRow toRow(MockPlace place, double priceHome, String home, String local) {
        String subtitle = getString(R.string.suggestion_subtitle, place.getKind(),
                place.getRating(), place.getDistanceKm(),
                CurrencyMock.format(place.getPriceLocal(), local));
        return UiRow.withAction(place.getName(), subtitle,
                CurrencyMock.format(priceHome, home), getString(R.string.action_add),
                () -> addToItinerary(place));
    }

    private void addToItinerary(MockPlace place) {
        repository.addItineraryItem(tripId, place.getName(), targetDay, null, null, false,
                result -> {
                    if (result.isSuccess()) {
                        toast(getString(R.string.suggestion_added, place.getName(), targetDay));
                    } else {
                        toast(R.string.error_save_failed);
                    }
                });
    }
}
